package dev.ethicaltrading.test;
import java.util.concurrent.CompletableFuture;
/** Test-only latency injection. The original disk result is never changed or synthesized. */
public final class EntityLoadGate {
    public static boolean armed;
    public static boolean intercepted;
    public static CompletableFuture<Void> ready = CompletableFuture.completedFuture(null);
    public static void arm() { armed=true; intercepted=false; ready=new CompletableFuture<>(); }
    public static void release() { armed=false; ready.complete(null); }
}
