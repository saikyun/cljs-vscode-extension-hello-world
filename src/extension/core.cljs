(ns extension.core
  (:require
   [clojure.string :as str]
   ["vscode" :as vscode]
   ["child_process" :as cp]
   ["os" :as os]
   ["fs" :as fs]
   ["path" :as path]))

(def ast (atom nil))

(defn ffilter [pred coll]
  (first (filter pred coll)))

(defn slurp [path]
  (fs/readFileSync (path/resolve js/__dirname path) "utf8"))

(defn gen-rand-ast-filename []
  (str/join "-" [(rand-int 100000) "ast.json"]))

(def json->clj (comp #(js->clj % :keywordize-keys true) js/JSON.parse))

(defn ->ast [_]
  (when-let [editor vscode/window.activeTextEditor]
    (when (= "shards" (some-> editor  .-document .-languageId))
      (let [shards-filename-path (.-path (vscode/workspace.getConfiguration "shards"))
            tmpdir (os/tmpdir)
            rand-ast-path (str/join "/" [tmpdir (gen-rand-ast-filename)])
            cmd (str/join " " [shards-filename-path "ast" vscode/window.activeTextEditor.document.fileName "-o" rand-ast-path])]
        (.exec cp cmd #js {:cwd tmpdir}
               (fn [a b c]
                 (println a b c)
                 (->> rand-ast-path slurp json->clj (swap! ast assoc vscode/window.activeTextEditor.document.fileName))
                 (fs/unlink rand-ast-path (fn [err] (println err)))))))))

(defn ->location
  [doc a-range]
  #js {"range" a-range
       "uri" (.-uri doc)})

(defn ->range [{:keys [start-pos end-pos]}]
  (new vscode/Range 
       (:line start-pos)
       (:column start-pos)
       (:line end-pos)
       (:column end-pos)))

(defn ->wire-pos
  [ast wire-name]
  (some->> ast
           :statements
           (keep (comp :blocks :Pipeline))
           (mapcat identity)
           (filter #(ffilter (fn [m] (= wire-name (some-> m :value :Identifier :name)))
                             (-> % :content :Func :params)))
           (ffilter #(= "wire" (some-> % :content :Func :name :name)))
           :line_info))

(defn handle-goto-def [ast]
  (fn [^js doc ^js pos _]
    (let [word-range (.getWordRangeAtPosition doc pos #"[a-z_][a-zA-Z0-9_.-]*")
          word (.getText doc word-range)
          {:keys [line column]} (->wire-pos (get @ast vscode/window.activeTextEditor.document.fileName) word)]
      (->location doc
                  (->range
                    {:start-pos {:line (dec line) :column (+ column 5)}
                     :end-pos {:line (dec line) :column (+ column 5 (count word))}})))))

(defn reload []
  (.log js/console "Reloading...")
  (js-delete js/require.cache (js/require.resolve "./extension")))

(defn activate [context]
  (doto ^js (.-subscriptions context)
    (.push (vscode/window.onDidChangeVisibleTextEditors (fn [e] (println "onDidChangeVisibleTextEditors") (->ast e))))
    (.push (vscode/window.onDidChangeActiveTextEditor (fn [e] (println "onDidChangeActiveTextEditor") (->ast e))))
    (.push (vscode/window.onDidChangeWindowState (fn [e] (println "onDidChangeWindowState") (->ast e))))
    (.push (vscode/workspace.onDidSaveTextDocument (fn [e] (println "onDidSaveTextDocument") (->ast e))))
    (.push (vscode/languages.registerDefinitionProvider "shards" #js {:provideDefinition (handle-goto-def ast)}))))

(defn deactivate [])

(def exports #js {:activate activate
                  :deactivate deactivate})
