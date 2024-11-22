# Jadescript

The agent-oriented programming language for [JADE](https://jade.tilab.com) agents.

In agent-oriented programming, software systems are composed of several autonomous, independent, and distributed entities (_agents_) interacting asynchronously by exchanging messages.

Jadescript comes with a dedicated [Eclipse](https://www.eclipse.org) IDE Plug-in that provides graphical support to develop and run agents.

## Resources
 * [Eclipse Plugin Download](https://github.com/aiagents/jadescript/releases/download/v1.0.20230504/Jadescript_v1.0.20230504.zip)
 * [Compiler Download](https://github.com/aiagents/jadescript/releases/download/v1.0.20230504/jadescriptc.jar) (not needed if you use the above plug-in)
 * [Programmer's Guide](https://github.com/aiagents/jadescript/releases/download/v1.0.20221015/JadescriptProgrammersGuide_v1.0.20221015.pdf)

## Citation
If you want to cite Jadescript, please cite the following paper:

Federico Bergenti, Giovanni Caire, Stefania Monica, Agostino Poggi. The first twenty years of agent-based software development with JADE. _Autonomous Agents and Multi-Agent Systems_, 34(2):36, 2020.

## Example
In this example, two agents (_Ping_ and _Pong_) repeatedly exchange messages back and forth.

Agents declarations are introduced by the `agent` keyword.

```
agent Ping
    uses ontology PingPong

    on create do
        log "Agent 'Ping' created."
        activate WaitFromPong
        activate SendRequest
    
    
agent Pong
    uses ontology PingPong

    on create do
        log "Agent 'Pong' created."
        activate WaitAndReply
```
Jadescript agents use _ontologies_ to manipulate concepts and to reason about actions and logical propositions/predicates. Ontologies are also important to ensure that two agents using the same ontology share the same interpretation of the contents of their messages.

```
ontology PingPong
    action Reply
    proposition Received
```
The tasks of agents (e.g., sending or reacting to messages, schedule activities...) are defined with behaviours.

```
cyclic behavior WaitAndReply 
    for agent Pong
    
    on message request Reply do
        sender = sender of message
        log "received 'Reply' request from agent '"  
            + name of sender + "'"
        inform Received to sender


one shot behavior SendRequest
    for agent Ping
    
    on execute do
        log "sending 'Reply' request..."
        send message request Reply to "Pong"@


cyclic behaviour WaitFromPong
    for agent Ping
    
    on message inform Received do
        sender = sender of message
        log "received 'Received' response from agent '" 
            + name of sender + "'"
        activate SendRequest after "PT1S" as duration
```

To learn how to set up, develop, and run this example (and others), refer to the [Jadescript Programmer's Guide](https://github.com/aiagents/jadescript/releases/download/v1.0.20221015/JadescriptProgrammersGuide_v1.0.20221015.pdf).

## Build
### Clone and setup Eclipse Workspace

1. `git clone https://github.com/aiagents/jadescript.git`;
2. Open Eclipse and select/create a workspace;
3. Select _File_ > _Import ..._ > _General_ > _Existing Projects into Workspace_  and click _Next_;
4. Choose _Select root directory_ and fill in the blank with the location of the directory `[...clone directory...]/jadescript/Jadescript`;
5. Make sure that all the found projects are selected;
6. Complete the import wizard by clicking _Finish_ and waiting for workspace building (you will see errors in the projects, but they will disappear with the following steps);
7. In the Package Explorer, navigate to _it.unipr.ailab.jadescript_ > _src_ > package `it.unipr.ailab.jadescript` > right click on `Jadescript.xtext` > _Run As_ > _Generate Xtext Artifacts_; 
8. Wait for the build process to finish.

Note: when launching the runtime Eclipse instance, remember to re-import the example projects (which can be found in `[...git clone dir...]/jadescript/runtime-EclipseXtext/`).

### Deploying the compiler as a stand-alone JAR
Run the ANT script `it.unipr.ailab.jadescript/createStandaloneJar.ant`. In Eclipse, simply right-click the file and select _Run As_ > _Ant Build_. The resulting JAR file will be located in `[...clone dir...]/StandaloneCompilerJar/`.

If you REALLY don't want to use eclipse, follow these steps to compile with the library from command line:

- If you don't already have Jade, download it from official sources: https://jade.tilab.com/ and extract the bin archive

- download the standalone jar for jadescript from above and place it in the lib/ directory inside the jade installation (you should now see both a jade.jar and a jadescriptc.jar inside the folder)

- create a src directory in your project folder and wite all your .jade files inside it (or in subdirectories if your project is big enough)

- compile the jade files into java by moving OUTSIDE of your src directory and running the command 

```
java -jar <path-to-jade-installation>/lib/jadescriptc.jar ./src/*.jade
```

This will create a new src-gen folder containing your agents and behaviors transalted into java

- create a bin directory, to store your .class files, then, while remaining in your project root folder (it should contain the src,src-gen and bin folder) compile your newly produced java files with

```
javac -cp .:<path-to-jade-installation>/lib/jadescriptc.jar:<path-to-jade-installation>/lib/jade.jar -d ./bin src-gen/*.java
```

You can now finally run your jade agent simulation with

```
java -cp .:<path-to-jade-installation>lib/jade.jar:<path-to-jadescript-installation>lib/jadescriptc.jar jade.Boot -gui -agents myAgent:MyAgentClass
```

### Exporting the Eclipse plug-in install file
In Eclipse, right-click on the `it.unipr.ailab.jadescript.feature`, then click on _Export..._.
In the dialog window, select _Plug-in Development_ > _Deployable features_.
In the following wizard, make sure that:

* in the _Destination_ tab, _Archive file_ is selected;
* in the _Options_ tab, _Categorize repository_ is enabled, and that the field at its right points to `[...clone dir...]/Jadescript/it.unipr.ailab.jadescript.repository/category.xml`.

Then click on _Finish_.

### Deploying the support JAR
The project `it.unipr.ailab.jadescript`, `it.unipr.ailab.jadescript.ui` and all the examples in `runtime-EclipseXtext` require the JAR file `jadescript.jar` both in the build path and in the classpath for the plugin runtime. 
The JAR file is created by the Eclipse project `it.unipr.ailab.jadescript.lib`.

If any of the contents of this project is modified, you need to export the corresponding JAR from the project. 
When exporting with Eclipse, export the JAR file to the directory `outJar` in the project directory (i.e. `it.unipr.ailab.jadescript.lib/outJar/jadescript.jar`).
Then navigate with the terminal to the root directory of the repository and run `./addLibJARs.sh`, which will copy the new JAR file to all the other project directories and to the directories of the example projects that require it.

## Repository Structure

```
├── jadescript/ - Repo root
   ├─ Jadescript/ - Language projects original workspace
   │   ├ it.unipr.ailab.jadescript/            - Main Project (grammar, semantics, mwe2 workflow...)
   │   ├ it.unipr.ailab.jadescript.ui/         - Eclipse UI project (editor, wizards, icons, run as...)
   │   ├ it.unipr.ailab.jadescript.ide/        - Required by .ui
   │   ├ it.unipr.ailab.jadescript.lib/        - Project for the jadescript.jar artifact
   │   ├ it.unipr.ailab.jadescript.tests/      - JUnit tests for the compiler
   │   ├ it.unipr.ailab.jadescript.stdlibgen/  - Project used to programmatically generate Jadescript builtin libraries (WIP)
   │   ├ it.unipr.ailab.jadescript.repository/ - Used to generate the plugin installation .zip
   │   └ it.unipr.ailab.jadescript.feature/    - Used to generate the plugin installation .zip 
   ├─ runtime-EclipseXtext/ - Examples/Tests workspace
   │   ├ PingPong/                             - Example: Simple ping-pong with a counter, between two agents
   │   ├ AlarmClock/                           - Example: Time-based agent behaviour
   │   ├ MusicShop/                            - Example: A set of buyers negotiate music items (CDs...) with sellers
   │   ├ AsynchronousBacktracking/             - Example: ABT implementation inspired from the algoithm in Shoham's book
   │   ├ HelloWorld/                           - Example: The classic "HelloWorld!" output
   │   ├ EngishAuction/                        - Example: An auction with an Auctioneer agent and several Bidder agents
   │   ├ Examples/                             - Example: Other small, single-source-file examples
   │   ├ Tests/                                - Test: Project to test the Jadescript plugin
   └─ addLibJARs.sh - BASH script used to automate the update/copy of dependency JAR files into example projects
	
```
