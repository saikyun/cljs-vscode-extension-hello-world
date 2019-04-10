# Minimal CLJS VSCode extension using shadow-cljs

This is a CLJS translation of the "Hello World Minimal Sample" for Visual Studio Code.
Original repo: https://github.com/Microsoft/vscode-extension-samples/tree/master/helloworld-minimal-sample

## Installation
Clone this repo.

Open the repo-folder in vscode (`File > Open`).

In a terminal (`Terminal > New Terminal`), do the following:

Install [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html#_installation):\
`npm install -g shadow-cljs`

And it's dependencies:\
`npm install --save-dev shadow-cljs`

The `package.json` already contains the dependencies, but I wanted you to do this step, in case you later decide to create a new extension!

Fetch the other dependencies:\
`npm install`

Compile (this takes a couple of second, later we'll learn how to reduce the compile time by using `watch`!):\
`shadow-cljs compile dev`

Start debugging (`Debug > Start Debugging`) 
're asked, select "Node.js" as environment.

Run the command by opening the command palette (`View > Command Palette...`), then write "Hello World" and press Enter.

Voila!

## Steps to utilize live reloading using shadow-cljs

In the `extension.core` namespace we have a function called `activate`. In it we tell vscode to run a lambda function when the extension is activated. `activate` is only called once, so even if we edit the message and reload our code, we won't see any change. You can try this by doing the following:

0. Be debugging (i.e. where you left off after following the Installation-steps)
1. In same terminal as before (that is, **not** in the debug instance of vscode): `shadow-cljs watch dev`
2. Make a change in the call to `showInformationMessage`, e.g. `(showInformationMessage "Hella World!")`
3. Run the command "Hello World" from the command palette again.

You should see the same message as earlier.

As we are clojurians, we love us some dynamic development environments. The step to fix this is simple!

When you register the command, instead of registering a lambda, register a var instead!

In `core.cljs`:

```clojure
(defn hella-world [] (.. vscode.window (showInformationMessage "Hella World!")))
```

And modify the call to `registerCommand`:

```clojure
(.. vscode.commands
  (registerCommand
  "extension.helloWorld"
  #'hella-world))
```

We also need to remove the cached version of our script, add the following function as well:
```clojure
(defn reload
  []
  (.log js/console "Reloading...")
  (js-delete js/require.cache (js/require.resolve "./extension")))
```

And in `shadow-cljs.edn`, add the following:
```clojure
:builds
 {:dev {...
        :devtools {:after-load extension.core/reload}}}}
```

After saving both files, click the green `Restart`-symbol in the debugging interface (the one with the pause-button etc). Or if you can't find it, just stop debugging and start it again.

Now you can make changes to `hella-world`, and those changes will be available in the debugging instance of vscode. Try it out by changing the message to something less profane, and run the command "Hello World" again! :)

Make sure that your project compiles properly, you see this by looking in the terminal, where shadow-cljs is nice enough to tell you when something goes wrong.

Enjoy!

## But what about the REPL?

Since we're acting like spoilt kids, we're lucky to have [thheller](https://github.com/thheller) to provide for us. When you run `shadow-cljs watch dev`, you might in the terminal notice a line akin to: `shadow-cljs - nREPL server started on port 61155`. Great news!

1. Connect to the nREPL server using your favourite client. Since you're using vscode, maybe [Calva](https://marketplace.visualstudio.com/itemdetails?itemName=cospaia.clojure4vscode)? I've also tried [cider-mode](https://cider.readthedocs.io/en/latest/). Calva was the most simple though, so I recommend that.
2. Now, you might think that you can just fire away stuff like `(js/console.log "I'm the queen!")`, but sadly, your client will most likely just get mad at you. This is what I got:
   ```
   Syntax error compiling at (cljs-vscode-extension-hello-world:localhost:62584(clj)*<2>:47:14).
   No such namespace: js
   ```
   The issue? We're in jvm-land!
   
### We need to go deeper!
To go to CLJS-land, I found it easiest to:

1. Stop debugging
2. Add a dependency to `shadow-cljs.edn`:
    ```clojure
    :dependencies
    [[cider/cider-nrepl "0.21.0"]]
    ```
3. Write the following in a terminal (if in VSCode, preferably the one you start debugging from):
    ```bash
    $ shadow-cljs clj-repl
    => (shadow/watch :dev)
    => (shadow/repl :dev)
    ```
    With log messages, it will look something like this (note the nREPL port, you'll need it later):
    ```bash
    $ shadow-cljs clj-repl
    shadow-cljs - config: <...>/cljs-vscode-extension-hello-world/shadow-cljs.edn  cli version: 2.8.29  node: v8.15.0
    shadow-cljs - server version: 2.8.29 running at http://localhost:9630
    shadow-cljs - nREPL server started on port 55618
    shadow-cljs - REPL - see (help)
    To quit, type: :repl/quit
    [1:0]~shadow.user=> (shadow/watch :dev)
    [:dev] Configuring build.
    [:dev] Compiling ...
    [:dev] Build completed. (42 files, 2 compiled, 0 warnings, 3.45s)
    :watching
    [1:0]~shadow.user=> (shadow/repl :dev)
    [1:1]~cljs.user=>
    ```
    If you now try to eval something in your fresh cljs-repl, you'll might get an error such as: `No application has connected to the REPL server. Make sure your JS environment has loaded your compiled ClojureScript code.`

4. Start debugging again
5. Run the "Hello World" command (this loads your code into vscode)
6. Try evaling something at the cljs-repl in the terminal again. Now it should work. :)

### Now add the finishing strokes

1. Connect using your favourite repl client, e.g. [Calva](https://marketplace.visualstudio.com/itemdetails?itemName=cospaia.clojure4vscode) or [cider-mode](https://cider.readthedocs.io/en/latest/)*.
   1. Assuming Calva, you can use the command "Calva: Connect", either in the debugging instance (for that emacs feeling), or in the vscode instance that started debugging.
   2. It's important that you fill in the port you got from running `shadow-cljs clj-repl` before.
   3. Choose `:dev` when asked about build
2. When you have successfully connected with a cljs-repl client, you should be able to evaluate: `(js/console.log "I'm the queen!")` - the result is shown in the `Debug Console` of the vscode instance where you started debugging!
3. To really verify that it works, you could run the following in the repl:
   ```clojure
   (in-ns 'extension.core) ;;=> extension.core
   (hella-world) ;;=> #object[Promise [object Promise]]
   ```
   A notification should popup in the vscode instance running the plugin!

Phew, all in a lot of simple(?!) steps' work.

More information about shadow-cljs: https://shadow-cljs.github.io/docs/UsersGuide.html

\* If you try this in `cider-mode`, it's important to **not** press enter when connecting using the sibling repl. You have to explicitly write `:dev`. If you don't understand what I mean about sibling repl, check the shadow-cljs docs above.

## Thanks

Thanks to [sogaiu](https://github.com/sogaiu) for helping me getting started with CLJS. :)
