# Validation and publication notes

## Source preparation

The publication copy was taken from the PCBuilderPlus source folder in the author's thesis archive. The application source and catalogues are retained. The publication adds English documentation, a screenshot extracted from the thesis, repository metadata and regression tests.

The full thesis document, IDE configuration, generated output, user configurations and bundled Chromium binaries are not included. Source and configuration files were scanned for common credential formats, personal email addresses and local user paths; no matching credentials or personal paths were found. This is a publication check, not a comprehensive security audit.

## Automated checks

`mvn clean verify` runs ten offline regression tests covering:

- A compatible build, including exact physical-clearance boundaries.
- Incompatible sockets and DDR memory types.
- An oversized GPU.
- Safe, marginal and insufficient PSU capacity.
- Empty-build behaviour and component-based power estimation.
- Loading every bundled component catalogue.
- Untrusted-price fallback and trusted-price cache clearing.

An additional desktop smoke test can be requested with:

```sh
mvn -Dpcbuilder.uiTests=true test
```

It initialises JavaFX and loads the main, assembly guide, optimisation and tutorial views. It does not open the embedded browser or exercise external websites.

## Current verification status

The Maven dependencies resolved, but the local restricted Windows publishing environment raises `AccessDeniedException` during Java's dependency path resolution. Consequently a successful full compilation and test run has **not yet been verified** for this publication copy. The tests above describe the included suite, not a claim that it has passed.

FXML/XML files were parsed successfully as XML. The README screenshot was visually checked. Live marketplace retrieval, native Chromium operation and complete interactive workflows remain unverified in the publication environment.

The thesis's ten original manual scenarios are separate from the automated regression suite added here.
