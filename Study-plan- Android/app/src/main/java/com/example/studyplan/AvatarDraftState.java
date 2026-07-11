package com.example.studyplan;

/** Pure state model for a profile avatar edit transaction. */
public final class AvatarDraftState {
    private final String savedAvatarPath;
    private final int savedImageTint;
    private final int savedFrameColor;
    private final String avatarPath;
    private final int imageTint;
    private final int frameColor;

    private AvatarDraftState(String savedAvatarPath, int savedImageTint, int savedFrameColor,
                             String avatarPath, int imageTint, int frameColor) {
        this.savedAvatarPath = savedAvatarPath;
        this.savedImageTint = savedImageTint;
        this.savedFrameColor = savedFrameColor;
        this.avatarPath = avatarPath;
        this.imageTint = imageTint;
        this.frameColor = frameColor;
    }

    public static AvatarDraftState begin(String savedAvatarPath, int savedImageTint, int savedFrameColor) {
        return new AvatarDraftState(savedAvatarPath, savedImageTint, savedFrameColor,
                savedAvatarPath, savedImageTint, savedFrameColor);
    }

    public AvatarDraftState preview(String pendingAvatarPath, int pendingImageTint, int pendingFrameColor) {
        return new AvatarDraftState(savedAvatarPath, savedImageTint, savedFrameColor,
                pendingAvatarPath, pendingImageTint, pendingFrameColor);
    }

    public AvatarDraftState cancel() {
        return begin(savedAvatarPath, savedImageTint, savedFrameColor);
    }

    public AvatarDraftState save() {
        return begin(avatarPath, imageTint, frameColor);
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public int getImageTint() {
        return imageTint;
    }

    public int getFrameColor() {
        return frameColor;
    }
}
