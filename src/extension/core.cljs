(ns extension.core)

(def vscode (js/require "vscode"))

(defn reload []
  (.log js/console "Reloading...")
  (js-delete js/require.cache (js/require.resolve "./extension")))

(defn hella-world []
  (.. vscode.window (showInformationMessage "Hella World!")))

(defn activate [context]
  (.log js/console "Activating")
  (let [disposable (.. vscode.commands (registerCommand "extension.shards-lsp" #'hella-world))]
    (.. context.subscriptions (push disposable))))

(defn deactivate [])

(def exports #js {:activate activate
                  :deactivate deactivate})
