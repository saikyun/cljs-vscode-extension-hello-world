(ns extension.core
  (:require
   [clojure.string :as str]
   ["vscode" :as vscode]
   ["child_process" :as cp]
   ["os" :as os]
   ["fs" :as fs]
   ["path" :as path]))

(def ast (atom nil))

(def debounce-timeout (atom nil))

(defn spit [file-path content]
  (.writeFileSync fs file-path content))

(defn ffilter [pred coll]
  (first (filter pred coll)))

(defn slurp [path]
  (fs/readFileSync (path/resolve js/__dirname path) "utf8"))

(defn gen-rand-ast-filename [kind]
  (let [extension (if (= "code" kind) "shs" "json")]
    (str/join "-" [(rand-int 100000) kind extension])))

(defn debounce [debounce-store f delay]
  (js/clearTimeout @debounce-store)
  (reset! debounce-store
          (js/setTimeout f delay)))

(def json->clj (comp #(js->clj % :keywordize-keys true) js/JSON.parse))

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

(defn ->wires-ast-info [ast]
  (some->> ast
           :statements
           (keep (comp :blocks :Pipeline))
           (mapcat identity)
           (filter #(= "wire" (some-> % :content :Func :name :name)))
           (map #(hash-map :wire (some->> % :content :Func :params (keep (comp :name :Identifier :value)) (first))
                           :line-info (:line_info %)))))

(defn ->wire-location [doc {:keys [wire line-info]}] 
  (let [{:keys [line column]} line-info]
    {:wire wire
     :location (->location doc
                           (->range
                            {:start-pos {:line (dec line) :column (+ column 5)}
                             :end-pos {:line (dec line) :column (+ column 5 (count wire))}}))}))

(defn ->wire-locations [doc ast]
  (map (partial ->wire-location doc) (->wires-ast-info ast)))

(defn ->ast [ast]
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
                   (->> rand-ast-path slurp json->clj (->wire-locations doc) (swap! ast assoc-in [doc.fileName :wire-locations]))
                   (fs/unlink rand-ast-path (fn [err] (println err))))
                 (fs/unlink rand-shs-path (fn [err] (println err)))))))))

(defn handle-goto-def [ast]
  (fn [^js doc ^js pos _]
    (println "handle-goto-def. ast" ast)
    (when doc
      (let [word-range (.getWordRangeAtPosition doc pos #"[a-z_][a-zA-Z0-9_.-]*")
            word (.getText doc word-range)
            wire-locations (get-in @ast [doc.fileName :wire-locations])]
        (:location (ffilter #(= word (:wire %)) wire-locations))))))

(defn ->debounced-ast [ast]
  (debounce debounce-timeout #(->ast ast) 300))

(defn reload []
  (.log js/console "Reloading...")
  (js-delete js/require.cache (js/require.resolve "./extension")))

(defn activate [context]
  (doto ^js (.-subscriptions context)
    (.push (vscode/window.onDidChangeVisibleTextEditors (fn [_] (println "onDidChangeVisibleTextEditors") (->ast ast))))
    (.push (vscode/window.onDidChangeActiveTextEditor (fn [_] (println "onDidChangeActiveTextEditor" (->ast ast)))))
    (.push (vscode/window.onDidChangeWindowState (fn [_] (println "onDidChangeWindowState" (->ast ast)))))
    (.push (vscode/workspace.onDidChangeTextDocument (fn [_] (println "onDidChangeTextDocument") (->debounced-ast ast))))
    (.push (vscode/workspace.onDidSaveTextDocument (fn [_] (println "onDidSaveTextDocument" (->ast ast)))))
    (.push (vscode/languages.registerDefinitionProvider "shards" #js {:provideDefinition (handle-goto-def ast)}))))

(defn deactivate [])

(def exports #js {:activate activate
                  :deactivate deactivate})
