# Contributing to FDC3 Java API

This repository is part of the [FDC3 project](https://fdc3.finos.org) at FINOS. It provides a Java implementation of the [FDC3 Standard](https://fdc3.finos.org/docs/api/spec) APIs and related libraries, and is maintained alongside the main [FDC3 repository](https://github.com/finos/FDC3).

Contributors to this repository are subject to the same FINOS contribution requirements as the broader FDC3 project. For the overall FDC3 contribution policy, issue workflows, and Working Group processes, see [Contributing to FDC3](https://github.com/finos/FDC3/blob/main/CONTRIBUTING.md). This document supplements that policy with guidance specific to this repository.

The project is also governed by:

* [Linux Foundation Antitrust Policy](https://www.linuxfoundation.org/antitrust-policy/)
* FINOS [IP Policy](https://community.finos.org/governance-docs/IP-policy.pdf)
* FINOS [Code of Conduct](https://community.finos.org/docs/governance/code-of-conduct)
* FINOS [Collaborative Principles](https://community.finos.org/docs/governance/collaborative-principles/)
* FINOS [Meeting Procedures](https://community.finos.org/docs/governance/meeting-procedures/)

FDC3 Java API is [Apache 2.0 licensed](LICENSE) and accepts contributions via Git pull requests.

## Contributor License Agreement

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those participants with an active, executed Individual Contributor License Agreement (ICLA) with FINOS, _OR_ who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the [Linux Foundation EasyCLA tool](https://easycla.lfx.linuxfoundation.org/#/).

*Need an ICLA? Unsure if you are covered under an existing CCLA? Email [help@finos.org](mailto:help@finos.org).*

## Contribution Process

Before making a contribution, please take the following steps:

1. Check whether there's already an open issue related to your proposed contribution. If there is, join the discussion and propose your contribution there.
2. If there isn't already a relevant issue, create one describing your contribution and the problem you're trying to solve.
3. Respond to any questions or suggestions raised in the issue by other developers.
4. Fork the project repository and prepare your proposed contribution.
5. Submit a pull request.

## Issue Lifecycle

### Prerequisites

* Have you [searched for duplicates](https://github.com/finos-labs/fdc3-java-api/issues)? A simple search for exception error messages or a summary of the unexpected behaviour should suffice.
* Are you using the latest version of the project?
* Are you sure this is a bug or missing capability?

### Issue Creation

* Create your issue [here](https://github.com/finos-labs/fdc3-java-api/issues/new).
* Choose the most appropriate issue template: bug report, feature request, or support question.
* Please use [Markdown formatting](https://help.github.com/categories/writing-on-github/) liberally to assist in readability.
* Use [code fences](https://help.github.com/articles/creating-and-highlighting-code-blocks/) for exception stack traces and log entries.

### Discussion

Issues that change the project usually benefit from discussion. You can post comments directly on the issue, ask for it to be added to an FDC3 meeting agenda by emailing [fdc3@finos.org](mailto:fdc3@finos.org), sending a message to the [#fdc3 channel on the FINOS Slack](https://finos-lf.slack.com/messages/fdc3/), or tag the FDC3 maintainers (`@finos/fdc3-maintainers`) in your issue, as described in [Contributing to FDC3](https://github.com/finos/FDC3/blob/main/CONTRIBUTING.md).

Issues should be connected to the pull request that resolves them. Prefix your pull request's description with a keyword and issue number, e.g. `resolves #123`. For more details see [GitHub's documentation](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue).

## How to Contribute a Patch

**1. Fork the repo**

(<https://github.com/finos-labs/fdc3-java-api/fork>)

**2. Create your feature branch**

`git checkout -b feature/fooBar`

**3. Commit your changes**

`git commit -am 'Describe what you changed'`

**4. Push to the branch**

`git push origin feature/fooBar`

**5. Create a Pull Request**

For help creating a pull request from your fork, [see GitHub's documentation](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork).

## Contribution Guidelines

To make review of PRs easier, please:

* Make sure your PR will merge cleanly. PRs that don't are unlikely to be accepted.
* For code contributions, follow the existing code layout and include the [Apache License v2.0 header](http://www.apache.org/licenses/LICENSE-2.0#apply) on new source files.
* Keep commits small and cohesive. If you have multiple contributions, submit them as independent commits and, ideally, as independent PRs.
* Reference issues if your PR has anything to do with an issue, even if it doesn't directly address it.
* Minimize non-functional changes, such as unnecessary whitespace changes.
* If necessary, due to third-party dependency licensing requirements, update the [NOTICE file](./NOTICE) with any new attribution or other notices.
* Run `mvn clean install` locally before submitting.

### Commit and PR Messages

* Reference issues, wiki pages, and pull requests liberally.
* Use the present tense ("Add feature" not "Added feature").
* Use the imperative mood ("Move button left..." not "Moves button left...").
* Limit the first line to 72 characters or less.

## Governance

Governance of the FDC3 Standard and Working Group is defined in the [FDC3 Governance document](https://github.com/finos/FDC3/blob/main/GOVERNANCE.md) and [Contributing to FDC3](https://github.com/finos/FDC3/blob/main/CONTRIBUTING.md). The sections below describe governance for this repository specifically.

### Roles

The project community consists of Contributors and Maintainers:

* A **Contributor** is anyone who submits a contribution to the project. Contributions may include code, issues, comments, documentation, media, or any combination of the above.
* A **Maintainer** is a Contributor who, by virtue of their contribution history, has been given write access to project repositories and may merge approved contributions.
* The **Lead Maintainer** is the project's interface with the FINOS team and Board. They are responsible for approving [quarterly project reports](https://community.finos.org/docs/governance/#project-governing-board-reporting) and communicating on behalf of the project. The Lead Maintainer is elected by a vote of the Maintainers.

The current Maintainer roster is listed in [MAINTAINERS.md](./MAINTAINERS.md).

### Contribution Rules

Anyone is welcome to submit a contribution to the project. The rules below apply to all contributions. The key words **MUST**, **SHALL**, **SHOULD**, **MAY**, etc. in this document are to be interpreted as described in [IETF RFC 2119](https://www.ietf.org/rfc/rfc2119.txt).

* All contributions **MUST** be submitted as pull requests, including contributions by Maintainers.
* All pull requests **SHOULD** be reviewed by a Maintainer other than the Contributor before being merged.
* Pull requests for non-trivial contributions **SHOULD** remain open for a review period sufficient to give all Maintainers an opportunity to review and comment.
* After the review period, if no Maintainer objects to the pull request, any Maintainer **MAY** merge it.
* If any Maintainer objects to a pull request, the Maintainers **SHOULD** try to reach consensus through discussion. If no consensus can be reached, any Maintainer **MAY** call for a vote on the contribution.

### Maintainer Voting

The Maintainers **MAY** hold votes only when they are unable to reach consensus on an issue. Any Maintainer **MAY** call a vote on a contested issue. Votes **SHALL** take the form of:

* `+1` — agree
* `-1` — disagree
* `+0` — abstain

Issues **SHALL** be decided by the majority of votes cast. If there is only one Maintainer, they **SHALL** decide any issue otherwise requiring a vote.

The Maintainers **SHALL** decide the following matters by consensus or, if necessary, a vote:

* Contested pull requests
* Election and removal of the Lead Maintainer
* Election and removal of Maintainers

All Maintainer votes **MUST** be carried out transparently, with all discussion and voting occurring in public using one of the following methods:

* Comments associated with the relevant issue or pull request, if applicable
* The project mailing list or another official public communication channel
* A regular, minuted project meeting

Votes on maintainer matters may also use the [Vote issue template](.github/ISSUE_TEMPLATE/Vote.md).

### Maintainer Qualifications

Any Contributor who has made a substantial contribution to the project **MAY** apply or be nominated to become a Maintainer. The existing Maintainers **SHALL** decide whether to approve the nomination according to the Maintainer Voting process described above.

### Maintainer List

All changes to the Maintainer list are managed publicly:

* Any addition, removal, or update **MUST** be submitted as a **pull request** to `MAINTAINERS.md`.
* If the change requires a vote, the vote outcome **MUST** be documented in or linked from the pull request description or comments.
* Whenever `MAINTAINERS.md` is updated with a change to maintainership, please email **[help@finos.org](mailto:help@finos.org)**.

### Changes to This Document

This document **MAY** be amended by a vote of the Maintainers according to the Maintainer Voting process above.
