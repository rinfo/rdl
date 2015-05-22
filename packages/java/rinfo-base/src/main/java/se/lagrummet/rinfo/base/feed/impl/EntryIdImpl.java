package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.Feed;

/**
 * Created by christian on 5/22/15.
 */
public class EntryIdImpl implements Feed.EntryId {
    String id;

    public EntryIdImpl(String id) {
        this.id = id;
    }

    @Override
    public String toString() {return id;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryIdImpl entryId = (EntryIdImpl) o;

        if (id != null ? !id.equals(entryId.id) : entryId.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
