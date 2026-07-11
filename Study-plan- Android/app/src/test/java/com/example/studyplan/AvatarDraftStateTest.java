package com.example.studyplan;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AvatarDraftStateTest {

    @Test
    public void cancelRestoresSavedAvatarAndColors() {
        AvatarDraftState initial = AvatarDraftState.begin("old.png", 0xFF1A73E8, 0xFFFFFFFF);
        AvatarDraftState pending = initial.preview("pending.png", 0xFFFF4081, 0xFF00A0FF);

        AvatarDraftState restored = pending.cancel();

        assertEquals("old.png", restored.getAvatarPath());
        assertEquals(0xFF1A73E8, restored.getImageTint());
        assertEquals(0xFFFFFFFF, restored.getFrameColor());
    }

    @Test
    public void saveReturnsPendingAvatarAndColors() {
        AvatarDraftState initial = AvatarDraftState.begin("old.png", 0xFF1A73E8, 0xFFFFFFFF);
        AvatarDraftState pending = initial.preview("pending.png", 0xFFFF4081, 0xFF00A0FF);

        AvatarDraftState saved = pending.save();

        assertEquals("pending.png", saved.getAvatarPath());
        assertEquals(0xFFFF4081, saved.getImageTint());
        assertEquals(0xFF00A0FF, saved.getFrameColor());
    }

    @Test
    public void previewDoesNotChangeSavedBaseline() {
        AvatarDraftState initial = AvatarDraftState.begin("old.png", 0xFF1A73E8, 0xFFFFFFFF);
        AvatarDraftState pending = initial.preview("pending.png", 0xFFFF4081, 0xFF00A0FF);

        assertEquals("old.png", pending.cancel().getAvatarPath());
        assertEquals(0xFF1A73E8, pending.cancel().getImageTint());
        assertEquals(0xFFFFFFFF, pending.cancel().getFrameColor());
    }
}
