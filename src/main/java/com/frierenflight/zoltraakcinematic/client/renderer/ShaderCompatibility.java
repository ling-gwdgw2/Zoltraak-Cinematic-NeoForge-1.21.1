package com.frierenflight.zoltraakcinematic.client.renderer;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

public final class ShaderCompatibility {
   private static final BooleanSupplier ACTIVE = findShaderApi("isShaderPackInUse");
   private static final BooleanSupplier SHADOW = findShaderApi("isRenderingShadowPass");
   private static final java.util.function.IntSupplier DEPTH = findDepthSupplier();
   private static boolean warned;

   public static boolean useFullDetailPass() {
      return ACTIVE.getAsBoolean();
   }

   public static boolean isShadowPass() {
      return SHADOW.getAsBoolean();
   }

   public static int getShaderDepthTexture() {
      if (!useFullDetailPass()) {
         return 0;
      }
      try {
         return DEPTH.getAsInt();
      } catch (Throwable t) {
         return 0;
      }
   }

   private static java.util.function.IntSupplier findDepthSupplier() {
      String[] irisClassNames = new String[]{"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"};
      for (String irisName : irisClassNames) {
         try {
            Class<?> irisClass = Class.forName(irisName);
            Method getPipelineManager = irisClass.getMethod("getPipelineManager");
            Object pipelineManager = getPipelineManager.invoke(null);
            if (pipelineManager == null) continue;
            Method getPipelineNullable = pipelineManager.getClass().getMethod("getPipelineNullable");

            return new java.util.function.IntSupplier() {
               private Class<?> cachedPipelineClass;
               private java.lang.reflect.Field cachedRenderTargetsField;
               private Method cachedGetDepthTexture;

               @Override
               public int getAsInt() {
                  try {
                     Object pipeline = getPipelineNullable.invoke(pipelineManager);
                     if (pipeline == null) return 0;

                     Class<?> pClass = pipeline.getClass();
                     if (pClass != cachedPipelineClass) {
                        cachedPipelineClass = pClass;
                        cachedRenderTargetsField = null;
                        cachedGetDepthTexture = null;

                        Class<?> cur = pClass;
                        while (cur != null && cur != Object.class) {
                           try {
                              cachedRenderTargetsField = cur.getDeclaredField("renderTargets");
                              cachedRenderTargetsField.setAccessible(true);
                              break;
                           } catch (NoSuchFieldException ignored) {
                              cur = cur.getSuperclass();
                           }
                        }
                     }

                     if (cachedRenderTargetsField == null) return 0;
                     Object renderTargets = cachedRenderTargetsField.get(pipeline);
                     if (renderTargets == null) return 0;

                     if (cachedGetDepthTexture == null) {
                        cachedGetDepthTexture = renderTargets.getClass().getMethod("getDepthTexture");
                     }
                     Object result = cachedGetDepthTexture.invoke(renderTargets);
                     return result instanceof Integer i ? i : 0;
                  } catch (Throwable t) {
                     return 0;
                  }
               }
            };
         } catch (Throwable ignored) {
         }
      }
      return () -> 0;
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
