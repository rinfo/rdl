package se.lagrummet.rinfo.store.depot;

import org.apache.commons.lang.StringUtils;


public class FileDepotCmdTool {

    public static enum Command {
        INDEX {
            void run(Depot depot) throws Exception {
                DepotSession session = depot.openSession();
                session.generateIndex();
                session.close();
            }
        };
        abstract void run(Depot depot) throws Exception;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println(String.format(
                    "Expected arguments: <config-properties> <properties-subset> <%s>",
                    StringUtils.join(Command.values(), "|").toLowerCase()));
            System.exit(0);
        }

        String propertiesPath = args[0];
        String subsetPrefix = args[1];
        Depot depot = DepotUtil.depotFromConfig(propertiesPath, subsetPrefix);

        String cmdName = args[2];

        try {
            Command command = Command.valueOf(cmdName.toUpperCase());
            command.run(depot);
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown command: " + cmdName);
            System.exit(1);
        }
    }

}
