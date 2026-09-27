package com.frierenflight.zoltraakcinematic.client.renderer;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

public final class ShaderCompatibility {
   private static final BooleanSupplier ACTIVE = findShaderApi("isShaderPackInUse");
   private static final BooleanSupplier SHADOW = findShaderApi("isRenderingShadowPass");
   private static boolean warned;

   public static boolean useFullDetailPass() {
      return ACTIVE.getAsBoolean();
   }

   public static boolean isShadowPass() {
      return SHADOW.getAsBoolean();
   }

   private static BooleanSupplier findShaderApi(String method) {
      String[] classNames = new String[]{"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"};
      for (String name : classNames) {
         try {
            Class<?> api = Class.forName(name);
            Method instance = api.getMethod("getInstance");
            Method active = api.getMethod(method);
            return () -> {
               try {
                  return Boolean.TRUE.equals(active.invoke(instance.invoke(null)));
               } catch (ReflectiveOperationException | LinkageError ex) {
                  return unavailable(ex, method.equals("isShaderPackInUse"));
               }
            };
         } catch (ClassNotFoundException ignored) {
         } catch (ReflectiveOperationException | LinkageError ex) {
            return () -> unavailable(ex, method.equals("isShaderPackInUse"));
         }
      }
      return () -> false;
   }

   private static boolean unavailable(Throwable ex, boolean defaultValue) {
      if (!warned) {
         warned = true;
         System.err.println("[ZoltraakCinematic] Optional shader API call unavailable: " + ex.getMessage());
      }
      return defaultValue;
   }

   private ShaderCompatibility() {}
}
