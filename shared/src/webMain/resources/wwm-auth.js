(function () {
    "use strict";

    var firebaseVersion = "12.16.0";
    var storageKeys = {
        uid: "web_auth_uid",
        idToken: "web_auth_id_token",
        provider: "web_auth_provider",
        displayName: "web_auth_display_name",
        email: "web_auth_email",
        isAnonymous: "web_auth_is_anonymous",
        changeId: "web_auth_change_id",
        errorId: "web_auth_error_id",
        errorMessage: "web_auth_error_message"
    };
    var changeSequence = 0;
    var signInInProgress = false;

    function setBridgeReady(isReady) {
        document.documentElement.setAttribute("data-wwm-auth-ready", isReady ? "true" : "false");
    }

    function nextId() {
        changeSequence += 1;
        return Date.now().toString(36) + "-" + changeSequence.toString(36);
    }

    function setOptionalStorageValue(key, value) {
        if (typeof value === "string" && value.trim() !== "") {
            window.localStorage.setItem(key, value);
        } else {
            window.localStorage.removeItem(key);
        }
    }

    function clearStoredSession() {
        window.localStorage.removeItem(storageKeys.uid);
        window.localStorage.removeItem(storageKeys.idToken);
        window.localStorage.removeItem(storageKeys.provider);
        window.localStorage.removeItem(storageKeys.displayName);
        window.localStorage.removeItem(storageKeys.email);
        window.localStorage.removeItem(storageKeys.isAnonymous);
    }

    function markActionCompleted() {
        window.localStorage.setItem(storageKeys.changeId, nextId());
    }

    function userProviderId(user) {
        if (user.isAnonymous) {
            return "firebase-anonymous";
        }
        var provider = (user.providerData || []).find(function (item) {
            return item && item.providerId && item.providerId !== "firebase";
        });
        return provider ? provider.providerId : "firebase";
    }

    async function storeUserSession(user) {
        var idToken = await user.getIdToken();
        window.localStorage.setItem(storageKeys.uid, user.uid);
        window.localStorage.setItem(storageKeys.idToken, idToken);
        window.localStorage.setItem(storageKeys.provider, userProviderId(user));
        window.localStorage.setItem(storageKeys.isAnonymous, String(user.isAnonymous));
        setOptionalStorageValue(storageKeys.displayName, user.displayName);
        setOptionalStorageValue(storageKeys.email, user.email);
        markActionCompleted();
    }

    function firebaseErrorMessage(error) {
        switch (error && error.code) {
            case "auth/popup-closed-by-user":
            case "auth/cancelled-popup-request":
                return "로그인 창이 닫혔어요. 다시 시도해주세요.";
            case "auth/popup-blocked":
                return "브라우저가 로그인 창을 차단했어요. 팝업을 허용한 뒤 다시 시도해주세요.";
            case "auth/network-request-failed":
                return "네트워크 연결을 확인한 뒤 다시 시도해주세요.";
            case "auth/unauthorized-domain":
                return "현재 웹 주소가 Firebase 승인 도메인에 등록되지 않았어요.";
            case "auth/operation-not-allowed":
                return "Firebase에서 이 로그인 방식을 먼저 활성화해주세요.";
            case "auth/account-exists-with-different-credential":
                return "같은 이메일로 가입된 다른 로그인 방식이 있어요. 기존 방식으로 로그인해주세요.";
            case "auth/requires-recent-login":
                return "보안을 위해 다시 로그인한 뒤 시도해주세요.";
            default:
                return "로그인을 처리하지 못했어요. 잠시 후 다시 시도해주세요.";
        }
    }

    function credentialFromAuthError(firebaseAuth, error) {
        return firebaseAuth.GoogleAuthProvider.credentialFromError(error)
            || firebaseAuth.OAuthProvider.credentialFromError(error);
    }

    function storeActionError(error) {
        window.localStorage.setItem(storageKeys.errorMessage, firebaseErrorMessage(error));
        window.localStorage.setItem(storageKeys.errorId, nextId());
    }

    function isMobileBrowser() {
        if (navigator.userAgentData && typeof navigator.userAgentData.mobile === "boolean") {
            return navigator.userAgentData.mobile;
        }
        return /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent);
    }

    function bindAction(id, action) {
        var element = document.getElementById(id);
        if (!element) {
            throw new Error("Missing web auth action: " + id);
        }
        element.addEventListener("click", function () {
            action().catch(storeActionError);
        });
    }

    function sameOriginAuthConfig(firebaseConfig) {
        var config = Object.assign({}, firebaseConfig);
        var isLocalhost = /^(?:localhost|127\.0\.0\.1|\[::1\])$/i.test(window.location.hostname);
        if (window.location.protocol === "https:" && !isLocalhost) {
            config.authDomain = window.location.host;
        }
        return config;
    }

    async function initializeWebAuth() {
        var firebaseConfig = window.__WWM_FIREBASE_CONFIG__;
        if (!firebaseConfig || !firebaseConfig.apiKey || !firebaseConfig.projectId) {
            setBridgeReady(false);
            return;
        }

        var modules = await Promise.all([
            import("https://www.gstatic.com/firebasejs/" + firebaseVersion + "/firebase-app.js"),
            import("https://www.gstatic.com/firebasejs/" + firebaseVersion + "/firebase-auth.js")
        ]);
        var firebaseApp = modules[0];
        var firebaseAuth = modules[1];
        var app = firebaseApp.initializeApp(sameOriginAuthConfig(firebaseConfig));
        var auth = firebaseAuth.getAuth(app);
        auth.languageCode = "ko";
        await firebaseAuth.setPersistence(auth, firebaseAuth.browserLocalPersistence);

        try {
            var redirectResult = await firebaseAuth.getRedirectResult(auth);
            if (redirectResult && redirectResult.user) {
                await storeUserSession(redirectResult.user);
            }
        } catch (error) {
            var redirectCredential = credentialFromAuthError(firebaseAuth, error);
            if (redirectCredential) {
                var existingResult = await firebaseAuth.signInWithCredential(auth, redirectCredential);
                await storeUserSession(existingResult.user);
            } else {
                storeActionError(error);
            }
        }

        await auth.authStateReady();
        if (auth.currentUser) {
            await storeUserSession(auth.currentUser);
        }

        function providerFor(providerId) {
            if (providerId === "google.com") {
                return new firebaseAuth.GoogleAuthProvider();
            }
            var provider = new firebaseAuth.OAuthProvider("apple.com");
            provider.addScope("email");
            provider.addScope("name");
            provider.setCustomParameters({ locale: "ko" });
            return provider;
        }

        async function signIn(providerId) {
            if (signInInProgress) {
                return;
            }
            signInInProgress = true;
            try {
                var provider = providerFor(providerId);
                if (isMobileBrowser()) {
                    if (auth.currentUser && auth.currentUser.isAnonymous) {
                        await firebaseAuth.linkWithRedirect(auth.currentUser, provider);
                    } else {
                        await firebaseAuth.signInWithRedirect(auth, provider);
                    }
                    return;
                }
                var result;
                try {
                    if (auth.currentUser && auth.currentUser.isAnonymous) {
                        result = await firebaseAuth.linkWithPopup(auth.currentUser, provider);
                    } else {
                        result = await firebaseAuth.signInWithPopup(auth, provider);
                    }
                } catch (error) {
                    if (error && error.code === "auth/popup-blocked") {
                        if (auth.currentUser && auth.currentUser.isAnonymous) {
                            await firebaseAuth.linkWithRedirect(auth.currentUser, provider);
                        } else {
                            await firebaseAuth.signInWithRedirect(auth, provider);
                        }
                        return;
                    }
                    if (error && (error.code === "auth/credential-already-in-use" || error.code === "auth/email-already-in-use")) {
                        var existingCredential = credentialFromAuthError(firebaseAuth, error);
                        if (existingCredential) {
                            result = await firebaseAuth.signInWithCredential(auth, existingCredential);
                        } else {
                            throw error;
                        }
                    } else {
                        throw error;
                    }
                }
                await storeUserSession(result.user);
            } finally {
                signInInProgress = false;
            }
        }

        bindAction("wwm-auth-google", function () {
            return signIn("google.com");
        });
        bindAction("wwm-auth-apple", function () {
            return signIn("apple.com");
        });
        bindAction("wwm-auth-refresh", async function () {
            if (auth.currentUser) {
                await storeUserSession(auth.currentUser);
            } else {
                markActionCompleted();
            }
        });
        bindAction("wwm-auth-sign-out", async function () {
            await firebaseAuth.signOut(auth);
            clearStoredSession();
            markActionCompleted();
        });
        bindAction("wwm-auth-delete-account", async function () {
            if (auth.currentUser) {
                await firebaseAuth.deleteUser(auth.currentUser);
            }
            clearStoredSession();
            markActionCompleted();
        });

        setBridgeReady(true);
    }

    window.wwmAuthReady = initializeWebAuth().catch(function () {
        setBridgeReady(false);
    });
})();
