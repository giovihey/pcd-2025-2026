package main

import (
	"fmt"
)

func main() {
	fmt.Println("Hello world!")
	matchRounds := 3
	numsPlayer := 1 << matchRounds

	var channels []chan Msg

	for i := 0; i < numsPlayer; i++ {
		playerId := fmt.Sprintf("player-%d", i)
		ch := addPlayer(playerId)

		/* collecting player channels */
		channels = append(channels, ch)
	}

	/* receiving messages */
	for i := 0; i < len(channels); i++ {
		msg := <-channels[i]
		fmt.Printf("%d from %s\n", msg.value, msg.playerId)
	}
}
