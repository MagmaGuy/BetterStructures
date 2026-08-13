package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.magmacore.util.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collapses the per-block "Unsupported key" spam Minecraft's data converter produces while reading
 * schematics saved by older versions.
 * <p>
 * Shipped .schem files carry the Minecraft DataVersion they were saved at, so WorldEdit runs every
 * block entity in them through Mojang's DataFixerUpper. Entries whose type no longer exists in the
 * running version — {@code minecraft:bed} is the common one, block entities for beds went away in
 * 1.13 when bed colour moved into the block state — come back as a failed
 * {@code DataResult}, which the server logs at ERROR, once per occurrence. A full content install
 * produces 424 identical lines per load.
 * <p>
 * Nothing is wrong: the entry carried no information the current format needs, the block itself
 * converts fine, and the structure pastes correctly. Reported as hundreds of ERROR lines it reads
 * like a broken install, so it is counted instead and reported as a single line naming the distinct
 * keys involved.
 * <p>
 * This is third-party output, so the only place to intervene is the logging backend. The filter is
 * installed for the duration of a load on the logger configuration that serves DataFixerUpper — the
 * nearest ancestor config, which is ROOT on stock servers — and removed afterwards, so it verifies
 * the event's exact logger name before denying anything.
 * Everything is guarded: a server whose logging backend is not log4j-core keeps its original output
 * rather than failing to load schematics.
 */
public final class SchematicConversionLog {
    private static final String DATA_FIXER_LOGGER_NAME = "com.mojang.datafixers.DataFixerUpper";
    private static final String UNSUPPORTED_KEY_PREFIX = "Unsupported key: ";

    private SchematicConversionLog() {
    }

    /**
     * Starts suppressing and counting conversion noise. Close the returned session when the load
     * finishes to restore normal logging and emit the summary.
     */
    public static Session capture() {
        return new Session();
    }

    public static final class Session implements AutoCloseable {
        private final UnsupportedKeyFilter filter = new UnsupportedKeyFilter();
        private LoggerConfig filteredLoggerConfig;
        private boolean closed;

        private Session() {
            try {
                if (!(LogManager.getContext(false) instanceof LoggerContext context)) return;
                LoggerConfig dataFixerLoggerConfig =
                        context.getConfiguration().getLoggerConfig(DATA_FIXER_LOGGER_NAME);
                filter.start();
                dataFixerLoggerConfig.addFilter(filter);
                filteredLoggerConfig = dataFixerLoggerConfig;
            } catch (Throwable throwable) {
                //A logging backend this cannot drive is not a reason to fail a content load; the
                //only consequence is that the original per-occurrence output stays.
                close();
            }
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            try {
                if (filteredLoggerConfig != null) filteredLoggerConfig.removeFilter(filter);
                filter.stop();
            } catch (Throwable throwable) {
                //Nothing actionable: the filter is inert once the load is over either way.
            }
            filteredLoggerConfig = null;
            report();
        }

        private void report() {
            long suppressed = filter.suppressed.get();
            if (suppressed == 0) return;
            Set<String> keys = new TreeSet<>(filter.keys);
            Logger.info("Skipped " + suppressed
                    + (suppressed == 1 ? " unsupported legacy block entry (" : " unsupported legacy block entries (")
                    + String.join(", ", keys) + ") while converting schematics saved by older "
                    + "Minecraft versions. This is expected and does not affect structures.");
        }
    }

    private static final class UnsupportedKeyFilter extends AbstractFilter {
        private final AtomicLong suppressed = new AtomicLong();
        private final Set<String> keys = Collections.newSetFromMap(new ConcurrentHashMap<>());

        @Override
        public Result filter(LogEvent event) {
            //getLoggerConfig lands on the nearest ancestor logger config (ROOT on stock servers),
            //so this filter can see other loggers' events; the name check below is the real boundary.
            if (event == null || event.getLevel() == null
                    || !event.getLevel().isMoreSpecificThan(Level.WARN)
                    || !DATA_FIXER_LOGGER_NAME.equals(event.getLoggerName())) {
                return Result.NEUTRAL;
            }
            if (event.getMessage() == null) return Result.NEUTRAL;
            String message = event.getMessage().getFormattedMessage();
            if (message == null || !message.startsWith(UNSUPPORTED_KEY_PREFIX)) return Result.NEUTRAL;

            suppressed.incrementAndGet();
            //Bounded on purpose: the point of the summary is to name what was skipped, and a
            //schematic set only ever produces a handful of distinct legacy types.
            if (keys.size() < 32) keys.add(message.substring(UNSUPPORTED_KEY_PREFIX.length()).trim());
            return Result.DENY;
        }
    }
}
