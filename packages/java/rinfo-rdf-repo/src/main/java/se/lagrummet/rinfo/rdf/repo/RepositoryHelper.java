package se.lagrummet.rinfo.rdf.repo;


public class RepositoryHelper {

	public static void main(String[] args) {
		try {
			if (args.length <= 1
					|| (!args[0].equals("clean") && !args[0].equals("setup"))
					|| (!args[1].equals("local") && !args[1].equals("remote"))) {
				System.out.println("Usage: [clean|setup] [local|remote]");
				System.exit(1);				
			}			
			if (args[0].equals("clean")) {
				if (args[1].equals("local")) {
					System.out.println("Cleaning local repository...");
					RepositoryUtil.cleanLocalRepository();
					System.out.println("Done.");
				} else if (args[1].equals("remote")) {
					System.out.println("Cleaning remote repository...");
					RepositoryUtil.cleanRemoteRepository();					
					System.out.println("Done.");
				}
			} else if (args[0].equals("setup")) {
				if (args[1].equals("local")) {
					System.out.println("Setup local repository...");
					RepositoryUtil.setupLocalRepository();
					System.out.println("Done.");
				} else if (args[1].equals("remote")) {
					System.out.println("Setup remote repository...");
					RepositoryUtil.setupRemoteRepository();
					System.out.println("Done.");
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
