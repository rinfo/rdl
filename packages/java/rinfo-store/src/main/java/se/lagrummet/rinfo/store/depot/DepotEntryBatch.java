package se.lagrummet.rinfo.store.depot;

import java.util.*;


public class DepotEntryBatch extends AbstractCollection<DepotEntry> {

    FileDepot depot;
    private TreeSet<EntryRef> ascDateSortedEntryRefs;

    public DepotEntryBatch(FileDepot depot) {
        this.depot = depot;
        ascDateSortedEntryRefs = new TreeSet<EntryRef>(
                new Comparator<EntryRef>() {
                    public int compare(EntryRef a, EntryRef b) {
                        if (a.date.equals(b.date)) {
                            return a.uriPath.compareTo(b.uriPath);
                        }
                        return a.date.compareTo(b.date);
                    }
            });
    }

    @Override
    public boolean add(DepotEntry depotEntry) {
        // only keeping necessary data to minimize memory use
        return ascDateSortedEntryRefs.add(new EntryRef(depotEntry));
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof DepotEntry) {
            EntryRef ref = new EntryRef(((DepotEntry)o));
            return ascDateSortedEntryRefs.contains(ref);
        } else {
            return false;
        }
    }

    @Override
    public Iterator<DepotEntry> iterator() {
        final Iterator<EntryRef> sortedIter = ascDateSortedEntryRefs.iterator();
        return new Iterator<DepotEntry>() {
            public boolean hasNext() {
                return sortedIter.hasNext();
            }
            public DepotEntry next() {
                EntryRef entryRef = sortedIter.next();
                DepotEntry depotEntry = depot.getUncheckedDepotEntry(entryRef.uriPath);
                if (depotEntry == null) {
                    throw new DepotUriException("Reference to entry with path <" +
                            entryRef.uriPath+"> yields no entry.");
                }
                return depotEntry;
            }
            public void remove() {
                sortedIter.remove();
            }
        };
    }

    @Override
    public int size() {
        return ascDateSortedEntryRefs.size();
    }

    private static class EntryRef {
        public EntryRef(DepotEntry depotEntry) {
            this.uriPath = depotEntry.getEntryUriPath();
            this.date = depotEntry.getUpdated();
        }
        String uriPath;
        Date date;
    }

}
