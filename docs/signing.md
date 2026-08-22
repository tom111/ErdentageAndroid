# App signing identity

The owner generated the permanent Erdentage Android app-signing identity on
2026-08-22. It has not yet signed a public artifact or been enrolled with a
store.

Public identity:

- Application ID: `de.eigenraum.erdentage`
- Alias: `erdentage-app`
- Container: PKCS#12
- Key: RSA 4096 bit
- Signature algorithm: SHA256withRSA
- Subject: `CN=Thomas Kahle, OU=Erdentage, O=Thomas Kahle, L=Magdeburg, C=DE`
- Validity: 2026-08-22 through 2126-07-29
- Certificate SHA-256 fingerprint:
  `82:7D:B4:BA:37:00:7F:3D:7C:FD:90:9B:C3:C6:9F:50:3A:EE:15:CD:13:DA:BC:0B:A0:1B:02:F6:E7:B1:8F:D6`
- Initial encrypted PKCS#12 SHA-256 checksum:
  `1b38710e5062d74fa47150d9738defe7e3e9e2d510bb92e433c739592c3b0e77`

The private key and its password must remain outside Git. Before the first
public artifact, the owner must confirm a recoverable password-manager copy and
an independent offline backup. Use this same signing identity for upstream
GitHub APKs, reproducible upstream-signed F-Droid APKs, and the Play App Signing
key. A separate Play upload key may be created when Play enrollment begins.

For every public release, record the source tag and commit, version name and
code, signing-tool version, artifact SHA-256 checksum, signing-certificate
fingerprint, and reproducibility result.
