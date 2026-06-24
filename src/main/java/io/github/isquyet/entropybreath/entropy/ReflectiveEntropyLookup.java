package io.github.isquyet.entropybreath.entropy;

import org.bukkit.Location;

import java.lang.reflect.Method;
import java.util.logging.Logger;

final class ReflectiveEntropyLookup implements EntropyLookup {
    private final Object entropyService;
    private final Method getEntropyMethod;
    private final Logger logger;
    private boolean warned;

    private ReflectiveEntropyLookup(Object entropyService, Method getEntropyMethod, Logger logger) {
        this.entropyService = entropyService;
        this.getEntropyMethod = getEntropyMethod;
        this.logger = logger;
    }

    static EntropyLookup create(Class<?> entropyServiceType, Object entropyService, Logger logger) throws NoSuchMethodException {
        return new ReflectiveEntropyLookup(entropyService, entropyServiceType.getMethod("getEntropy", Location.class), logger);
    }

    @Override
    public int getEntropy(Location location) {
        try {
            Object entropy = getEntropyMethod.invoke(entropyService, location);
            if (entropy instanceof Number number) {
                return Math.max(0, number.intValue());
            }
            warnOnce("EntropyService#getEntropy returned a non-numeric value; using 0.");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("Failed to read entropy from EntropyCore; using 0. Cause: " + exception.getMessage());
        }
        return 0;
    }

    private void warnOnce(String message) {
        if (warned) {
            return;
        }
        warned = true;
        logger.warning(message);
    }
}
