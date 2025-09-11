<!-- JITPACK BADGES:START -->
[![JitPack Latest](https://jitpack.io/v/BlueCodeSystems/opensrp-client-household.svg)](https://jitpack.io/#BlueCodeSystems/opensrp-client-household)
[![Build for latest tag (v2.1.0)](https://jitpack.io/v/BlueCodeSystems/opensrp-client-household/v2.1.0.svg)](https://jitpack.io/#BlueCodeSystems/opensrp-client-household/v2.1.0)
[![master-SNAPSHOT](https://jitpack.io/v/BlueCodeSystems/opensrp-client-household/master-SNAPSHOT.svg)](https://jitpack.io/#BlueCodeSystems/opensrp-client-household/master-SNAPSHOT)
<!-- JITPACK BADGES:END -->

# OpenSRP Client Household
OpenSRP Client Household Library - Fork of OpenSRP Client Family Module

### Get it from JitPack

This library is published via [JitPack](https://jitpack.io/#BlueCodeSystems/opensrp-client-household).

Add JitPack to your repositories and then depend on the artifact:

```groovy
// settings.gradle or top-level build.gradle
repositories {
  maven { url 'https://jitpack.io' }
}

// app/build.gradle (or your consuming module)
dependencies {
  // Use a released tag (see badges above for latest)
  implementation 'com.github.BlueCodeSystems:opensrp-client-household:2.1.0'

  // Or use the latest snapshot of master
  // implementation 'com.github.BlueCodeSystems:opensrp-client-household:master-SNAPSHOT'
}
```

### Publish

#### 1. Locally
Publish the AAR to your local Maven (`~/.m2/repository`) for downstream testing:

```bash
./gradlew :opensrp-household:publishToMavenLocal
```

Create a distributable ZIP (AAR + POM + sources/javadoc):

```bash
./gradlew :opensrp-household:packageReleaseZip
```

#### 2. Sonatype (Maven Central)
If you have credentials configured, you can publish snapshots/releases to Sonatype (s01):

```bash
# Snapshots
./gradlew :opensrp-household:publish \
  -PsonatypeUsername=YOUR_USER -PsonatypePassword=YOUR_TOKEN -PskipSigning=true

# For releases, provide signing and omit -PskipSigning
./gradlew :opensrp-household:publish \
  -PsonatypeUsername=YOUR_USER -PsonatypePassword=YOUR_TOKEN \
  -Psigning.gnupg.keyName=KEYID -Psigning.gnupg.passphrase=PASSPHRASE
```

#### 3. JitPack
JitPack builds the project on demand from tags/commits. Use the badges at the top to pick a version or master-SNAPSHOT. No credentials are required for consumers.

## Configurability

By placing a file named `app.properties` in your implementation assets folder (See sample app) , one can configure certain aspects of the app

### Configurable Settings

| Configuration                       | Type    | Default | Description                                   |  
| ----------------------------------- | ------- | ------- | ----------------------------------------------|  
| `family.head.first.name.enabled`         | Boolean | false    | Show First name of head of family next to family surname|  
