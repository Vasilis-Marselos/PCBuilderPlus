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

The public source was verified by [GitHub Actions run 36052794385](https://github.com/Vasilis-Marselos/PCBuilderPlus/actions/runs/36052794385) at source commit `debc0ff5f9c65f32087b943db8ac1b85a3db0b7a`:

- **Windows / Java 21:** `mvn clean verify` passed, including compilation, packaging and all ten offline regression tests.
- **Linux / Java 21:** the same clean build and ten offline tests passed.
- **Linux / virtual display:** the additional JavaFX smoke test passed, loading all four local views. All eleven tests passed in this run.

The desktop smoke test is opt-in and is skipped during the normal offline suite. It is not a complete interactive end-to-end test.

FXML/XML files were also parsed successfully as XML, and the README screenshot was visually checked. Live marketplace retrieval, native Chromium operation and complete interactive workflows remain unverified in the publication environment.

The thesis's ten original manual scenarios are separate from the automated regression suite added here.
