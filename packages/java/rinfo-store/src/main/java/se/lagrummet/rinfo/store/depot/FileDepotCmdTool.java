package se.lagrummet.rinfo.store.depot;

public class FileDepotCmdTool {

    public static final String COMMANDS = "index";
    // TODO:IMPROVE: more commands; e.g. check, ...

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println(String.format(
                    "Expected arguments: <config-file> <%s>", COMMANDS));
            System.exit(0);
        }
        FileDepot depot = FileDepot.newConfigured(args[0]);
        String command = args[1];
        if (command.equals("index")) {
            depot.generateIndex();
        } else {
            System.out.println("Unknown command: " + command);
            System.exit(1);
        }
    }

}
