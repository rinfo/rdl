package se.lagrummet.rinfo.rdf.repo.main;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;

import se.lagrummet.rinfo.rdf.repo.RepositoryFactory;
import se.lagrummet.rinfo.rdf.repo.util.ConfigurationUtil;
import se.lagrummet.rinfo.rdf.repo.util.RepositoryUtil;

/**
 * Main class for executable jar.
 *
 * @author msher
 */
public class RepositoryHelper {

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

            Configuration config = ConfigurationUtil.getDefaultConfiguration();
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

            if (args[0].equals("clean")) {
                System.out.println("RepositoryHelper: Cleaning repository...");
                RepositoryUtil.cleanRepository(config);

            } else if (args[0].equals("setup")) {
                System.out.println("RepositoryHelper: Setup of repository...");
                RepositoryUtil.setupRepository(config);

            } else if (args[0].equals("remove")) {
                if (args[1].equals("local")) {
                    System.out.println("Error: 'remove' is not implemented for local repository");
                    System.exit(1);
                }
                System.out.println("RepositoryHelper: Removing repository...");
                RepositoryUtil.removeRemoteRepository(config);

            } else if (args[0].equals("testdata")) {
                System.out.println("RepositoryHelper: Adding test statement to repository...");
                addTestStatement(config);
            }

            System.out.println("RepositoryHelper: Done.");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("static-access")
    private static void addTestStatement(Configuration config) throws Exception {
        RepositoryFactory factory = new RepositoryFactory(config);
        RepositoryConnection conn = null;
        try {
            conn = factory.getRepository().getConnection();
            String s = "http://example.org/s";
            String p = "http://example.org/p";
            String o = "http://example.org/o";
            Statement st = new StatementImpl(new URIImpl(s), new URIImpl(p), new URIImpl(o));
            conn.add(st);
        } finally {
            if (conn != null && conn.isOpen()) {
                conn.close();
            }
            if (factory != null) {
                factory.shutDown();
            }
        }
    }

}
