# Diddit

Diddit is a simple Reddit-like P2P social media platform.

## Setup

1. Create a file `.env` in the root directory with the following contents:
	```
	#!/usr/bin/env bash

	# Copy this file from ".env.sample" to ".env", then fill in these values
	# A Ditto AppID and Playground token can be obtained from https://portal.ditto.live
	DITTO_APP_ID="fc38a9f8-1743-4cc8-b5aa-d6697a57ac4e"
	DITTO_PLAYGROUND_TOKEN="dbeb0f5e-3aa8-46c4-baa0-34dd2edb0551"
	DITTO_AUTH_URL="https://i83inp.cloud.dittolive.app"
	DITTO_WEBSOCKET_URL="wss://i83inp.cloud.dittolive.app"
	```
2. Start the server using `./gradlew clean bootRun`
3. Navigate to http://localhost:8080/ in a web browser.

## Usage

> [!NOTE]
> If the site appears unresponsive when interacting with a post or navigating, please allow up to a minute for the stalled request to complete.

- Sign up for an account at http://localhost:8080/register (if you already have an account, sign in at http://localhost:8080/).
- Create a post by providing a message and an optional attachment.
- Click on a post to view its replies.
- Click *↑ Up* to return to the previous parent or click on the *Diddit* logo to return back to the homepage.
- Rate posts using the *Like* and/or *Dislike* buttons.
- Delete your own post (and all replies) using the *Delete* button.
- Filter posts by recency or rating using the filter options above the replies.
- Sign out using the *Sign out* button.

## Contact

- Dario Manser · d.manser@stud.unibas.ch
- Eda Erkek · eda.erkek@unibas.ch
- Marvin George · marvin.george@stud.unibas.ch
