package com.maxistar.textpad.recovery;

public final class RecoveryDraft {
    public final RecoveryMetadata metadata;
    public final String text;

    public RecoveryDraft(RecoveryMetadata metadata, String text) {
        this.metadata = metadata;
        this.text = text;
    }
}
