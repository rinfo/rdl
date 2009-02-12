package se.lagrummet.rinfo.rdf.repo.main;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;

import se.lagrummet.rinfo.rdf.repo.RepositoryHandler;

/**
 * Main class for executable jar.
 *
 * @author msher
 */
public class RepositoryHelper {

    private static final String PROPERTIES_FILE_NAME = "rinfo-rdf-repo.properties";

    public static final List<String> SUPPORTED_COMMANDS = Arrays.asList(
            "setup", "clean", "remove", "testdata");

    private static final String usage =
        "Usage: [setup|clean|remove|testdata] [local|remote] [repository id]";


    public static void main(String[] args) {
        try {
            if (args.length < 1
                    || (!SUPPORTED_COMMANDS.contains(args[0]))) {
                System.out.println(usage);
                System.exit(1);
            }

            // TODO: or supply config file via args.
            Configuration config = new PropertiesConfiguration(PROPERTIES_FILE_NAME);
            if (args.length > 1) {
                if (args[1].equals("local")) {
                    config.setProperty("use.local.repository", true);
                } else if (args[1].equals("remote")) {
                    config.setProperty("use.local.repository", false);
                } else {
                    System.out.println(usage);
                    System.exit(1);
                }
            }
            if (args.length > 2) {
                config.setProperty("repository.id", args[2]);
            }

            RepositoryHandler repoHandler = new RepositoryHandler(config);
            try {
                handleCommand(repoHandler, args[0]);
            } finally {
                repoHandler.shutDown();
            }

            System.out.println("RepositoryHelper: Done.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("static-access")
    private static void handleCommand(RepositoryHandler repoHandler, String cmd)
            throws Exception {
        if (cmd.equals("setup")) {
            System.out.println("RepositoryHelper: Setup of repository...");
            repoHandler.initialize();

        } else if (cmd.equals("clean")) {
            System.out.println("RepositoryHelper: Cleaning repository...");
            repoHandler.initialize();
            repoHandler.cleanRepository();

        } else if (cmd.equals("remove")) {
            System.out.println("RepositoryHelper: Removing repository...");
            repoHandler.initialize();
            repoHandler.removeRepository();

        } else if (cmd.equals("testdata")) {
            System.out.println("RepositoryHelper: Adding test statement to repository...");
            repoHandler.initialize();
            addTestStatement(repoHandler);
        }
    }

    @SuppressWarnings("static-access")
    private static void addTestStatement(RepositoryHandler repoHandler) throws Exception {
        RepositoryConnection conn = null;
        try {
            conn = repoHandler.getRepository().getConnection();
            String s = "http://example.org/s";
            String p = "http://example.org/p";
            String o = "http://example.org/o";
            Statement st = new StatementImpl(new URIImpl(s), new URIImpl(p), new URIImpl(o));
            conn.add(st);
        } finally {
            if (conn != null && conn.isOpen()) {
                conn.close();
            }
            if (repoHandler != null) {
                repoHandler.shutDown();
            }
        }
    }

}
