package org.ploudstore.ploudStorePlugin.model;

public class FetchResult {
    public enum Status { OK, RATE_LIMITED, FORBIDDEN, ERROR }

    private final Status status;
    private final PendingResponse response;

    private FetchResult(Status status, PendingResponse response) {
        this.status = status;
        this.response = response;
    }

    public static FetchResult ok(PendingResponse response) {
        return new FetchResult(Status.OK, response);
    }

    public static FetchResult rateLimited() {
        return new FetchResult(Status.RATE_LIMITED, null);
    }

    public static FetchResult forbidden() {
        return new FetchResult(Status.FORBIDDEN, null);
    }

    public static FetchResult error() {
        return new FetchResult(Status.ERROR, null);
    }

    public Status getStatus() { return status; }
    public PendingResponse getResponse() { return response; }
    public boolean isOk() { return status == Status.OK; }
}
