package io.github.microcks.operator.resources;

import io.github.microcks.operator.api.Microcks;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * @author laurent
 */
public class KeycloakInstallPrecondition implements Condition<HasMetadata, Microcks> {
   @Override
   public boolean isMet(Microcks primary, HasMetadata secondary, Context<Microcks> context) {
      return primary.getSpec().getKeycloak().isInstall();
   }
}
