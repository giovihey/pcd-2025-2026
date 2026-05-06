package main

// Match runs a single round of the Odds-and-Evens game.
//
// It reads one message from each channel — channel1 for player 1 (odds)
// and channel2 for player 2 (evens) — then determines the winner by
// summing their picked values:
//   - If the sum is odd,  player 1 wins.
//   - If the sum is even, player 2 wins.
//
// The result is sent back to each player via their respective reply channel:
// player 1 receives true if they won, player 2 receives the opposite.
//
// Match blocks until both players have sent their messages.
func Match(channel1 chan Msg, channel2 chan Msg) {
	player1 := <-channel1
	player2 := <-channel2

	sum := player1.value + player2.value
	player1Wins := sum%2 == 1

	player1.reply <- player1Wins
	player2.reply <- !player1Wins
}
