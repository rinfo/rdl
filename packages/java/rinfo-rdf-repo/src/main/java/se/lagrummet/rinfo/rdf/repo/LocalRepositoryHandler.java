package se.lagrummet.rinfo.rdf.repo;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.NotImplementedException;

import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.NotifyingSail;
import org.openrdf.sail.inferencer.fc.DirectTypeHierarchyInferencer;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.nativerdf.NativeStore;


/*
 *  NOTE: Local repositories are not handled by a RepositoryManager to
 *  enable what seems to be an easier way to connect to other triple stores
 *  using a SAIL connection, in contrast to the SailImplConfig used by a
 *  RepositoryManager. If possible though, a LocalRepositoryManager could
 *  be used for a more consistent implementation.
 */

public class LocalRepositoryHandler extends RepositoryHandler {

    private String dataDir;
    private Repository localRepository;

    public LocalRepositoryHandler(String dataDir, String repoId,
            String storeType, String inferenceType) throws Exception {
        super(repoId, storeType, inferenceType);
        this.dataDir = dataDir;
        if (storeType.equals("native") && StringUtils.isEmpty(dataDir)) {
            throw new Exception(
                    "Property dataDir must not be empty when using storeType 'native'.");
        }
    }

    public Repository getRepository() {
        return localRepository;
    }

    public void initialize() throws Exception {
        NotifyingSail sail = null;

        if (storeType.equals("memory")) {
            sail = new MemoryStore();
        } else if (storeType.equals("native")) {
            sail = new NativeStore(new File(dataDir + "/" + repoId));
        } else {
            throw new Exception("Unsupported store type: " + storeType);
        }

        if ("rdfs".equals(inferenceType)) {
            localRepository = new SailRepository(
                    new ForwardChainingRDFSInferencer(sail));
        } else if ("dt".equals(inferenceType)) {
            localRepository = new SailRepository(
                    new DirectTypeHierarchyInferencer(sail));
        } else {
            localRepository = new SailRepository(sail);
        }

        localRepository.initialize();
    }

    public synchronized void removeRepository() throws Exception {
        // TODO
        throw new NotImplementedException(
                "Error: 'remove' is not implemented for local repository");
    }

}
