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

(defn ->ast []
  (let [tmpdir (os/tmpdir)
        rand-ast-path (str/join "/" [tmpdir (gen-rand-ast-filename)])
        cmd (str/join " " ["cd" tmpdir ";" "shards" "ast" vscode/window.activeTextEditor.document.fileName "-o" rand-ast-path])]
    (.exec cp cmd (fn [_] (->> rand-ast-path slurp json->clj (reset! ast)) 
                          (fs/unlink rand-ast-path)))))

(defn ->location
  [^js doc a-range]
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
  (fn [^vscode/TextDocument doc pos _]
    (let [word-range (.getWordRangeAtPosition doc pos #"[a-z_][a-zA-Z0-9_.-]*")
          word (.getText doc word-range)
          {:keys [line column]} (->wire-pos @ast word)]
      (->location doc
                  (->range
                    {:start-pos {:line (dec line) :column (+ column 5)}
                     :end-pos {:line (dec line) :column (+ column 5 (count word))}})))))

(defn handle-change [_]
  (->ast))

(defn reload []
  (.log js/console "Reloading...")
  (js-delete js/require.cache (js/require.resolve "./extension")))

(defn activate [context]
  (->ast)
  (let [doc-change-provider (vscode/workspace.onDidSaveTextDocument handle-change)
        definition-provider (vscode/languages.registerDefinitionProvider
                              "shards"
                              #js {:provideDefinition (handle-goto-def ast)})]
    (doto context.subscriptions
      (.push definition-provider)
      (.push doc-change-provider))))

(defn deactivate [])

(def exports #js {:activate activate
                  :deactivate deactivate})
