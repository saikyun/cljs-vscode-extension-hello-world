(ns extension.core)

(def vscode (js/require "vscode"))

(defn activate
  [context]
  (.log js/console "Yeah boii")
  (let [disposable (.. vscode.commands
                      (registerCommand
                      "extension.helloWorld"
                      #(.. vscode.window (showInformationMessage "Hello World!"))))]
    (.. context.subscriptions (push disposable))))

(defn deactivate [])

(def exports #js {:activate activate
                  :deactivate deactivate})