# Minimal CLJS VSCode extension using shadow-cljs

This is a CLJS translation of the "Hello World Minimal Sample" for VSCode.
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

Compile:\
`shadow-cljs compile dev`

Start debugging (`Debug > Start Debugging`).

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

```
(defn hella-world [] (.. vscode.window (showInformationMessage "Hella World!")))
```

And modify the call to `registerCommand`:

```
(.. vscode.commands
  (registerCommand
  "extension.helloWorld"
  #'hella-world))
```

We also need to remove the cached version of our script, add the following function as well:
```
(defn reload [] (.log js/console "Reloading...")
  (js-delete js/require.cache (js/require.resolve "./extension")))
```

And in `shadow-cljs.edn`, add the following:
```
:builds
 {:dev {...
        :devtools {:after-load extension.core/reload}}}}
```

After saving both files, click the green `Restart`-symbol in the debugging interface (the one with the pause-button etc). Or if you can't find it, just stop debugging and start it again.

Now you can make changes to `hella-world`, and those changes will be available in the debugging instance of vscode. Try it out by changing the message to something less profane, and run the command "Hello World" again! :)

Make sure that your project compiles properly, you see this by looking in the terminal, where shadow-cljs is nice enough to tell you when something goes wrong.

Enjoy!

## Thanks

Thanks to [sogaiu](https://github.com/sogaiu) for helping me getting started with CLJS. :)
