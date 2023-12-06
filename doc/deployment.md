## Deployment

The deployment process is handled by [JitPack](https://jitpack.io/#beyondwords-io/player-android). A new version of the player sdk is deployed for each git tag.

The [release](../.github/workflows/release.yml) workflow, running on GitHub Actions, triggered on each GitHub release is used to publish the [Example application](./images/example-app.png) as an apk to the GitHub release assets.
