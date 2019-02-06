package de.viadee.xai.anchor.algorithm.execution;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 *
 */
public interface ExecutorServiceFunction extends Function<Integer, ExecutorService>, Serializable {
}
