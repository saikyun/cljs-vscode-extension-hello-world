(ns extension.core
  (:require
   [clojure.string :as str]
   ["vscode" :as vscode]
   ["child_process" :as cp]
   ["os" :as os]
   ["fs" :as fs]
   ["path" :as path]))

(def ast (atom nil))

(defn spit [file-path content]
  (.writeFileSync fs file-path content))

(defn ffilter [pred coll]
  (first (filter pred coll)))

(defn slurp [path]
  (fs/readFileSync (path/resolve js/__dirname path) "utf8"))

(defn gen-rand-ast-filename [kind]
  (let [extension (if (= "code" kind) "shs" "json")]
    (str/join "-" [(rand-int 100000) kind extension])))

(def json->clj (comp #(js->clj % :keywordize-keys true) js/JSON.parse))

(defn ->ast []
  (let [editor vscode/window.activeTextEditor
        doc (some-> editor .-document)
        text (some-> doc .getText)]
    (when (and editor
               (= "shards" (.-languageId doc))
               (not= (hash text) (get-in @ast [doc.fileName :code-hash])))
      (let [shards-filename-path (.-path (vscode/workspace.getConfiguration "shards"))
            tmpdir (os/tmpdir)
            rand-shs-path (str/join "/" [tmpdir (gen-rand-ast-filename "code")])
            rand-ast-path (str/join "/" [tmpdir (gen-rand-ast-filename "ast")])
            cmd (str/join " " [shards-filename-path "ast" rand-shs-path "-o" rand-ast-path])]
        (spit rand-shs-path text)
        (.exec cp cmd #js {:cwd tmpdir}
               (fn [error b c]
                 (println error b c)
                 (swap! ast assoc-in [doc.fileName :code-hash] (hash text))
                 (when (fs/existsSync rand-ast-path)
                   (->> rand-ast-path slurp json->clj (swap! ast assoc-in [doc.fileName :ast]))
                   (fs/unlink rand-ast-path (fn [err] (println err))))
                 (fs/unlink rand-shs-path (fn [err] (println err)))))))))

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
    (when doc
      (let [word-range (.getWordRangeAtPosition doc pos #"[a-z_][a-zA-Z0-9_.-]*")
            word (.getText doc word-range)
            {:keys [line column]} (->wire-pos (get-in @ast [doc.fileName :ast]) word)]
        (->location doc
                    (->range
                     {:start-pos {:line (dec line) :column (+ column 5)}
                      :end-pos {:line (dec line) :column (+ column 5 (count word))}}))))))

(defn reload []
  (.log js/console "Reloading...")
  (js-delete js/require.cache (js/require.resolve "./extension")))

(defn activate [context]
  (doto ^js (.-subscriptions context)
    (.push (vscode/window.onDidChangeVisibleTextEditors (fn [_] (println "onDidChangeVisibleTextEditors") (->ast))))
    (.push (vscode/window.onDidChangeActiveTextEditor (fn [_] (println "onDidChangeActiveTextEditor" (->ast)))))
    (.push (vscode/window.onDidChangeWindowState (fn [_] (println "onDidChangeWindowState" (->ast)))))
    (.push (vscode/workspace.onDidSaveTextDocument (fn [_] (println "onDidSaveTextDocument" (->ast)))))
    (.push (vscode/languages.registerDefinitionProvider "shards" #js {:provideDefinition (handle-goto-def ast)}))))

(defn deactivate [])

(def exports #js {:activate activate
                  :deactivate deactivate})
