/*
 * Copyright FINOS and Contributors to the FDC3 project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finos.fdc3.api.metadata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Verification outcomes for signed context objects, populated by the receiving app's
 * security layer after attempting signature verification.
 * <p>
 * Only {@link #isSigned()} is guaranteed to be present. A context may be signed without the
 * signature being either valid or trusted, so check {@link #getValid()} and
 * {@link #getTrusted()} before relying on the signer's identity.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageAuthenticity {

    private boolean signed;
    private Boolean valid;
    private Boolean trusted;
    private String alg;
    private String kid;
    private String jku;
    private AntiReplayClaims antiReplayClaims;
    private List<String> errors;

    public MessageAuthenticity() {
    }

    public MessageAuthenticity(boolean signed) {
        this.signed = signed;
    }

    /**
     * Indicates whether the context includes a signature, but check the other fields to see
     * if the signature is valid.
     */
    @JsonProperty("signed")
    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    /** True if the JWS cryptographically verifies against the signed bytes. */
    @JsonProperty("valid")
    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    /** True if the signing key was obtained from an approved/trusted source. */
    @JsonProperty("trusted")
    public Boolean getTrusted() {
        return trusted;
    }

    public void setTrusted(Boolean trusted) {
        this.trusted = trusted;
    }

    /** The signature algorithm used (from the JWS protected header). */
    @JsonProperty("alg")
    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    /** The key identifier used to sign the message (from the JWS protected header). */
    @JsonProperty("kid")
    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    /**
     * The JSON Web Key Set URL where the public key can be retrieved (from the JWS protected
     * header).
     */
    @JsonProperty("jku")
    public String getJku() {
        return jku;
    }

    public void setJku(String jku) {
        this.jku = jku;
    }

    @JsonProperty("antiReplayClaims")
    public AntiReplayClaims getAntiReplayClaims() {
        return antiReplayClaims;
    }

    public void setAntiReplayClaims(AntiReplayClaims antiReplayClaims) {
        this.antiReplayClaims = antiReplayClaims;
    }

    /** Human-readable diagnostics, if the Desktop Agent or security layer supplied any. */
    @JsonProperty("errors")
    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
