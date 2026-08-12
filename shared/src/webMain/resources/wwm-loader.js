(function () {
    function loadScript(src, onError) {
        var script = document.createElement("script");
        script.src = src;
        script.type = "application/javascript";
        script.onerror = onError;
        document.body.appendChild(script);
    }

    function setMetaContent(name, content) {
        var meta = document.querySelector('meta[name="' + name + '"]');
        if (meta && content) {
            meta.setAttribute("content", content);
        }
    }

    function startApp() {
        loadScript("whenwheremeet-wasm.js", function () {
            loadScript("whenwheremeet-js.js");
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
            setMetaContent("wwm-firebase-api-key", config.apiKey);
            setMetaContent("wwm-firebase-project-id", config.projectId);
        })
        .catch(function () {
            // Local development intentionally falls back to the local repository.
        })
        .then(startApp);
})();
