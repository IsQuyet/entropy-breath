package io.github.isquyet.entropybreath.command;

import io.github.isquyet.entropybreath.air.BreathingStatusProvider;
import io.github.isquyet.entropybreath.message.MessageService;

public record EntropyBreathCommandContext(
        MessageService messages,
        BreathingStatusProvider statusProvider,
        Runnable reloadCallback
) {
}
