/*
 * Copyright The Microcks Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.microcks.operator.model;

import io.github.microcks.operator.api.model.IngressSpec;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jboss.logging.Logger;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Holds utility methods to manage Ingress params from specification.
 * @author laurent
 */
public class IngressSpecUtil {

   /**
    * Get the ingress annotations if defined, null otherwise.
    * @param spec The IngressSpec that may be null
    * @return Ingress annotations to apply.
    */
   public static Map<String, String> getAnnotationsIfAny(IngressSpec spec) {
      if (spec != null && spec.getAnnotations() != null) {
         return spec.getAnnotations();
      }
      return null;
   }

   /**
    * Whether we should generate certificate Secret for this ingress.
    * @param spec The IngressSpec that may be null
    * @return True if we have to generate a self-signed secret holding certificate, false otherwise.
    */
   public static boolean generateCertificateSecret(IngressSpec spec) {
      return spec == null || (spec.getSecretRef() == null && spec.isGenerateCert());
   }

   /**
    * Get the ingress secret name to use from the spec or default.
    * @param spec              The IngressSpec that may be null
    * @param defaultSecretName The default name to apply
    * @return Secret name from the spec of default one
    */
   public static String getSecretName(IngressSpec spec, String defaultSecretName) {
      if (spec != null) {
         if (spec.getSecretRef() != null) {
            return spec.getSecretRef();
         }
         if (!spec.isGenerateCert()) {
            return null;
         }
      }
      return defaultSecretName;
   }

   /**
    * Generate a Secret holding a self-signed certificate and key for Ingress tests purposes.
    * @param name   The name of secret to generate
    * @param labels The labels to add to Secret
    * @param host   The host name to generate a cert and key for.
    * @return The created Secret to persist using Kube apis.
    */
   public static Secret generateSelfSignedCertificateSecret(String name, Map<String, String> labels, String host) {
      return generateSelfSignedCertificateSecret(name, labels, List.of(host));
   }

   /**
    * Generate a Secret holding a self-signed certificate and key for Ingress tests purposes.
    * @param name   The name of secret to generate
    * @param labels The labels to add to Secret
    * @param hosts  A list of host names to generate a cert and key for.
    * @return The created Secret to persist using Kube apis.
    */
   public static Secret generateSelfSignedCertificateSecret(String name, Map<String, String> labels, List<String> hosts) {
      Security.addProvider(new BouncyCastleProvider());

      X500Principal subject = new X500Principal("CN=" + hosts.get(0));
      X500Principal signedByPrincipal = subject;
      KeyPair keyPair = generateKeyPair();
      KeyPair signedByKeyPair = keyPair;

      long notBefore = System.currentTimeMillis();
      long notAfter = notBefore + (1000L * 3600L * 24 * 365);

      ASN1Encodable[] encodableAltNames = new ASN1Encodable[hosts.size()];
      for (int i=0; i<hosts.size(); i++) {
         encodableAltNames[i] = new GeneralName(GeneralName.dNSName, hosts.get(i));
      }
      KeyPurposeId[] purposes = new KeyPurposeId[] { KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth };

      X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(signedByPrincipal, BigInteger.ONE,
            new Date(notBefore), new Date(notAfter), subject, keyPair.getPublic());

      try {
         certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
         certBuilder.addExtension(Extension.keyUsage, true,
               new KeyUsage(KeyUsage.digitalSignature + KeyUsage.keyEncipherment));
         certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(purposes));
         certBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(encodableAltNames));

         final ContentSigner signer = new JcaContentSignerBuilder(("SHA256withRSA"))
               .build(signedByKeyPair.getPrivate());
         X509CertificateHolder certHolder = certBuilder.build(signer);

         return new SecretBuilder()
               .withNewMetadata()
                  .withName(name)
                  .addToLabels(labels)
               .endMetadata()
               .withType("kubernetes.io/tls")
               .addToStringData("tls.key", getPrivateKeyPkcs1Pem(keyPair))
               .addToStringData("tls.crt", getCertificatePem(certHolder))
               .build();
      } catch (Exception e) {
         Logger.getLogger(IngressSpecUtil.class).error(e.getMessage());
         throw new AssertionError(e.getMessage());
      }
   }

   private static KeyPair generateKeyPair() {
      try {
         KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
         keyPairGenerator.initialize(2048, new SecureRandom());
         return keyPairGenerator.generateKeyPair();
      } catch (GeneralSecurityException var2) {
         throw new AssertionError(var2);
      }
   }

   private static String getCertificatePem(X509CertificateHolder certHolder) throws IOException {
      StringBuilder result = new StringBuilder();
      result.append("-----BEGIN CERTIFICATE-----\n");
      encodeBase64Lines(result, certHolder.getEncoded());
      result.append("-----END CERTIFICATE-----\n");
      return result.toString();
   }

   private static String getPrivateKeyPkcs1Pem(KeyPair keyPair) throws IOException {
      PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(keyPair.getPrivate().getEncoded());
      StringBuilder result = new StringBuilder();
      result.append("-----BEGIN RSA PRIVATE KEY-----\n");
      encodeBase64Lines(result, privateKeyInfo.parsePrivateKey().toASN1Primitive().getEncoded());
      result.append("-----END RSA PRIVATE KEY-----\n");
      return result.toString();
   }

   private static void encodeBase64Lines(StringBuilder out, byte[] data) {
      String base64 = new String(Base64.getEncoder().encode(data));
      for (int i = 0; i < base64.length(); i += 64) {
         out.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
      }
   }
}
