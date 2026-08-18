(function () {
    var assetVersion = "20260817-app-icon-1";

    function versioned(src) {
        return src + "?v=" + assetVersion;
    }

    function loadScript(src) {
        return new Promise(function (resolve, reject) {
            var script = document.createElement("script");
            script.src = src;
            script.type = "application/javascript";
            script.onload = resolve;
            script.onerror = reject;
            document.body.appendChild(script);
        });
    }

    function setMetaContent(name, content) {
        var meta = document.querySelector('meta[name="' + name + '"]');
        if (meta && content) {
            meta.setAttribute("content", content);
        }
    }

    function startApp() {
        loadScript(versioned("/whenwheremeet-wasm.js")).catch(function () {
            return loadScript(versioned("/whenwheremeet-js.js"));
        });
    }

    fetch("/__/firebase/init.json")
        .then(function (response) {
            if (!response.ok) {
                throw new Error("Firebase Hosting config is unavailable.");
            }
            return response.json();
        })
        .then(function (config) {
            window.__WWM_FIREBASE_CONFIG__ = config;
            setMetaContent("wwm-firebase-api-key", config.apiKey);
            setMetaContent("wwm-firebase-project-id", config.projectId);
        })
        .catch(function () {
            // Local development intentionally falls back to the local repository.
        })
        .then(function () {
            return loadScript(versioned("/wwm-auth.js"));
        })
        .then(function () {
            return window.wwmAuthReady;
        })
        .catch(function () {
            // The login screen remains available so the user can retry after reconnecting.
        })
        .then(startApp);
})();
