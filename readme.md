# Headmate Labeller
Firstly, sorry for the confusing project name.

This is a simple discord bot which allows you to create a list of headmates in a specific channel.

This project may end up being integrated into my [general purpose plural bot](https://gitlab.com/Compass_System/compass-plural-bot).

Sample Output (for our pluralkit system):
![A header, "Compasses 🧭" followed by embeds for Lorielle Sinclaire, Teddy, Needle, Quint, and Ellie/World, showing pronouns, proxytags and sometimes the headmate name if display name doesn't match.](./resources/sample_output.png)
## Usage
For now, as I have not needed to use this project much I have just been running it inside Intellij IDEA.

Firstly you will need to create a run directory in the project root and then create a file inside that directory called `.env`

Inside that file you will need to specify the discord bot token like so:
```
TOKEN=your_token_here
```

Then you can run the project from inside Intellij IDEA, `gradlew :run`.

Once in the channel you would like to create the list in you can run the only command:
```
/create-list [url: url to system export] [ignore: optional comma seperated list of headmate id/names to ignore]
```
