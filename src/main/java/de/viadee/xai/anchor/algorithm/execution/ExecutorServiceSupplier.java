package de.viadee.xai.anchor.algorithm.execution;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 *
 */
public interface ExecutorServiceSupplier extends Supplier<ExecutorService>, Serializable {
}
