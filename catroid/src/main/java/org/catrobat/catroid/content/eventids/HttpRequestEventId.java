package org.catrobat.catroid.content.eventids;

import java.util.UUID;

public class HttpRequestEventId extends EventId {
    private static final long serialVersionUID = 1L;
    private final String uniqueId = UUID.randomUUID().toString();

    public HttpRequestEventId() { super(EventId.USER_CONCAT); }

    @Override public boolean equals(Object object) {
        return object instanceof HttpRequestEventId && uniqueId.equals(((HttpRequestEventId) object).uniqueId);
    }

    @Override public int hashCode() { return uniqueId.hashCode(); }
}
