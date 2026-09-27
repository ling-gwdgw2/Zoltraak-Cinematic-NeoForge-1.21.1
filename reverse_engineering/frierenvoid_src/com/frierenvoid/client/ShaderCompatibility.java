package com.frierenvoid.client;

import com.frierenvoid.VoidConfig;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

final class ShaderCompatibility {
   private static final BooleanSupplier ACTIVE = findShaderApi("isShaderPackInUse");
   private static final BooleanSupplier SHADOW = findShaderApi("isRenderingShadowPass");
   private static boolean warned;

   static boolean useFullDetailPass() {
      return switch ((VoidConfig.ShaderCompatibilityMode)VoidConfig.SHADER_COMPATIBILITY.get()) {
         case ON -> true;
         case OFF -> false;
         case AUTO -> ACTIVE.getAsBoolean();
      };
   }

   static boolean isShadowPass() {
      return SHADOW.getAsBoolean();
   }

   private static BooleanSupplier findShaderApi(String method) {
      String[] var1 = new String[]{"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"};
      int var2 = var1.length;
      int var3 = 0;

      while (var3 < var2) {
         String name = var1[var3];

         try {
            Class<?> api = Class.forName(name);
            Method instance = api.getMethod("getInstance");
            Method active = api.getMethod(method);
            return () -> {
               try {
                  return Boolean.TRUE.equals(active.invoke(instance.invoke(null)));
               } catch (ReflectiveOperationException | LinkageError ex) {
                  return unavailable(exx, method.equals("isShaderPackInUse"));
               }
            };
         } catch (ClassNotFoundException var8) {
            var3++;
         } catch (ReflectiveOperationException | LinkageError ex) {
            return () -> unavailable(ex, method.equals("isShaderPackInUse"));
         }
      }

      return () -> false;
   }

   private static boolean unavailable(Throwable ex, boolean defaultValue) {
      if (!warned) {
         warned = true;
         LogUtils.getLogger().warn("Frieren: optional shader API call unavailable", ex);
      }

      return defaultValue;
   }

   private ShaderCompatibility() {
   }
}
