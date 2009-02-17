package se.lagrummet.rinfo.rdf.repo;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;

/**
 * A Command Line for managing repositories.
 */
public class RepositoryCommandLine {

    private static final String DEFAULT_PROPERTIES_FILE_NAME = "rinfo-rdf-repo.properties";

    public static final List<String> SUPPORTED_COMMANDS = Arrays.asList(
            "setup", "clean", "remove", "testdata");

    private static final String usage =
        "Usage: [setup|clean|remove|testdata] <path-to-repo-properties-file>";


    public static void main(String[] args) {
        try {
            if (args.length < 1
                    || (!SUPPORTED_COMMANDS.contains(args[0]))) {
                System.out.println(usage);
                System.exit(1);
            }

            String propertiesPath = (args.length > 1)?
                    args[1] : DEFAULT_PROPERTIES_FILE_NAME;

            Configuration config = new PropertiesConfiguration(propertiesPath);
            RepositoryHandler repoHandler = RepositoryHandlerFactory.create(config);
            try {
                handleCommand(repoHandler, args[0]);
            } finally {
                repoHandler.shutDown();
            }

            System.out.println("Done.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("static-access")
    private static void handleCommand(RepositoryHandler repoHandler, String cmd)
            throws Exception {
        if (cmd.equals("setup")) {
            System.out.println("Setup of repository...");
            repoHandler.initialize();

        } else if (cmd.equals("clean")) {
            System.out.println("Cleaning repository...");
            repoHandler.initialize();
            repoHandler.cleanRepository();

        } else if (cmd.equals("remove")) {
            System.out.println("Removing repository...");
            repoHandler.initialize();
            repoHandler.removeRepository();

        } else if (cmd.equals("testdata")) {
            System.out.println("Adding test statement to repository...");
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
