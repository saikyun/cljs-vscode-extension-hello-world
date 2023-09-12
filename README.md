# vscode-shards ![License: BSD 3-Clause](https://img.shields.io/badge/license-BSD%203--Clause-blue.svg)

[Shards](https://github.com/fragcolor-xyz/shards) language Support in VSCode.

This extension will provide comprehensive support for the Shards programming language in Visual Studio Code. Developed in `ClojureScript` and `tmLanguage`, the extension will offer:

- [x] Syntax highlighting
- [x] Custom shards path
- [x] Go to definitions
  - [x] Wires
    - [ ] Cross-file
  - [ ] Variables
  - [ ] @define
- [ ] Go to references
- [x] Outline
  - [ ] Meshes with go to definitions
  - [x] Wire definitions with go to definitions
    - [ ] Variable definitions with go definitions
    - [ ] Wire references (activator type too?) with go to definitions
- [ ] Red file on:
  - [ ] no ast
  - [ ] duplicate wire definitions
- [ ] Red squiggle under duplicate wire definitions
- [ ] Warnings:
  - [ ] Shards executable wasn't found. Try to show only once, in case user only wants syntax highlighting.

## Install extension

Grab it from the [VS Code Marketplace](https://marketplace.visualstudio.com/items?itemName=fragcolor.shards), or issue the command:
```
code --install-extension fragcolor.shards
```
You could also copy this repo or just copy the relevant files to `<user home>/.vscode/extensions` folder in VS Code. Check the relevant files with `vsce ls`.

## Usage

1. Setup full path to shards executable in the settings: search for "shards"
1. Open any `.shs` file to activate the extension.

##  Development

Development using shadow-cljs and VS Code debugger.

Install [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html#_installation) command globally and local npm dependencies:
```bash
npm install -g shadow-cljs
npm install
```

Continuously reload changes:
```bash
shadow-cljs watch dev
```

### Debug extension

#### Start debugger
Start debugging this extension:

- Go to _Run_ *->* _Start Debugging_ to open a new window with your extension loaded.
- When asked, select "Node.js" and `:dev` build

#### Test functionality

**Syntax highlighting**: Verify code is highlighted and that the language configuration settings are working. Try with `shards*.shs` files. To inspect tokens and scopes for syntax highlighting, go to VS Code command palette, and run `Developer: Inspect Editor Tokens and Scopes`.

**Go to definition**: Try with `test*.shs` files.

**Shards path**: Go to settings, look for shards, setup a non-default path for shards and test with a **go to definition**.

#### Making changes

When making changes to `shards.tmLanguage.edn`, generate `shards.tmLanguage.json` using a tool like [jet](https://github.com/borkdude/jet), here as:
```bash
cat shards.tmLanguage.edn |jet --to=json > shards.tmLanguage.json
```

Test code prior to release:
```bash
shadow-cljs release dev
```
Useful to uncover issues that could arise from using `:simple` optimizations.

#### Reload extension

Reload (`Ctrl+R` or `Cmd+R` on Mac) the VS Code window with your extension to load your changes. Otherwise, relaunch the extension from the debug toolbar.

### Publish this extension

Build code for release:
```bash
shadow-cljs release dev
```

Make sure you're going to publish the required and only the required files:
```
vsce ls
```
As of writing, this is what we're publishing:
```
highlight/language-configuration.json
highlight/shards.tmLanguage.edn
highlight/shards.tmLanguage.json
LICENSE
logo.png
out/extension.js
package.json
README.md
```
Unsure yet if the source map at `out/extension.js.map` is working.

Bump the `version` number in `package.json` before publishing this extension, then:
```bash
vsce package
```
Finally, upload the generated `.vsix` file to the [Fragcolor's VS Code marketplace](https://marketplace.visualstudio.com/manage/publishers/fragcolor) if you're a member of the organization, or in your own stall.

## What's in the folder?

The `package.json` file is the entry point of the extension.

### tmLanguage syntax highlighting

The [highlight](highlight) folder contains all of the files necessary for syntax highlighting.

- `package.json` - this is the manifest file declaring language support and locating the grammar file.
- `shards.tmLanguage.json` - this is the Text mate grammar file that is used for tokenization, generated from `shards.tmLanguage.edn`.
- `language-configuration.json` - this is the language configuration, defining the tokens that are used for comments and brackets.

### LSP in ClojureScript

The [src](src) folder contains the code necessary to implement a Language Server Protocol (LSP).

In the `extension.core` namespace we have a function called `activate`. In it we tell vscode to run a function when the extension is activated. 

To re-run `activate`, click the green `Restart`-symbol in the debugging interface (the one with the pause-button etc).

Make sure that your project compiles properly, you see this by looking in the terminal, where `shadow-cljs` is nice enough to tell you when something goes wrong.

#### But what about the REPL?

We're lucky to have [thheller](https://github.com/thheller) provide for us. When you run `shadow-cljs watch dev`, you might notice the following in the terminal: `shadow-cljs - nREPL server started on port 61155`. Great news!

1. Connect to the nREPL server using your favourite client. Since you're using vscode, maybe [Calva](https://marketplace.visualstudio.com/itemdetails?itemName=cospaia.clojure4vscode)? I've also tried [cider-mode](https://cider.readthedocs.io/en/latest/). Calva was the most simple though, so I recommend that.
2. Now, you might think that you can just fire away stuff like `(js/console.log "I'm the queen!")`, but sadly, your client will most likely just get mad at you. This is what I got:
   ```
   Syntax error compiling at (cljs-vscode-extension-hello-world:localhost:62584(clj)*<2>:47:14).
   No such namespace: js
   ```
   The issue? We're in jvm-land!
   
#### We need to go deeper!
To go to CLJS-land, I found it easiest to:

1. Stop debugging
2. Add a dependency to `shadow-cljs.edn`:
```clojure
:dependencies [[cider/cider-nrepl "0.21.0"]]
```

1. Write the following in a terminal (if in VSCode, preferably the one you start debugging from):
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
If you now try to eval something in your fresh cljs-repl, you'll might get an error such as: `No application has connected to the REPL server. Make sure your JS environment has loaded your compiled ClojureScript code.

1. Start debugging again
2. Run the "Hello World" command (this loads your code into vscode)
3. Try evaling something at the cljs-repl in the terminal again. Now it should work. :)

#### Now add the finishing strokes

1. Connect using your favourite repl client, e.g. [Calva](https://marketplace.visualstudio.com/itemdetails?itemName=cospaia.clojure4vscode) or [cider-mode](https://cider.readthedocs.io/en/latest/).
2. Assuming Calva, you can use the command "Calva: Connect", either in the debugging instance (for that emacs feeling), or in the vscode instance that started debugging.
3. It's important that you fill in the port you got from running `shadow-cljs clj-repl` before.
4. Choose `:dev` when asked about build
5. When you have successfully connected with a cljs-repl client, you should be able to evaluate: `(js/console.log "I'm the queen!")` - the result is shown in the `Debug Console` of the vscode instance where you started debugging!
6. To really verify that it works, you could run the following in the repl:
[code, clojure]
```
(in-ns 'extension.core) ;=> extension.core
```
A notification should popup in the VS Code instance running the plugin.

More information about shadow-cljs: https://shadow-cljs.github.io/docs/UsersGuide.html

If you try this in `cider-mode`, it's important to **not** press enter when connecting using the sibling repl. You have to explicitly write `:dev`. If you don't understand what I mean about sibling repl, check the shadow-cljs docs above.

## Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

To share this extension with the world, read on about [publishing an extension](https://code.visualstudio.com/api/working-with-extensions/publishing-extension).

## Acknowledgements

This repo was originally forked from: [cljs-vscode-extension-hello-world](https://github.com/saikyun/cljs-vscode-extension-hello-world).

## [LICENSE](LICENSE)

_vscode-shards-syntax_ source code is licensed under the [BSD 3-Clause license](LICENSE).
