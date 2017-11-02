import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let host
let port
let prevCmd	// to keep track of previous command


cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username>[<host>][<port>]')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
	/*
	 * if user provides host and port, set to user selection,
	 * otherwise use default localhost and 8080 port
	 */
	if( args.length > 1 ) {
        username = args.username
        host      = args.host
        port      = args.port
	} else {
		username = args.username
		host = 'localhost'
		port = 8080
  }
  
  server = connect( {host: host, port: port}, () => {
    server.write(new Message({username, command: 'connect'}).toJSON() + '\n')
    callback()
  })
  

    server.on('data', (buffer) => {
      //this.log(Message.fromJSON(buffer).toString())
      let s = Message.fromJSON(buffer).toString()
      let c = Message.fromJSON(buffer).command

      if(c === 'connect'){
        this.log(cli.chalk['gray'](s))
      } else if (c === 'disconnect') {
        this.log(cli.chalk['red'](s))
      } else if (c === 'echo'){
      this.log(cli.chalk['yellow'](s))
      } else if (c === 'broadcast'){
        this.log(cli.chalk['cyan'](s))
      } else if (c === '@'){
        this.log(cli.chalk['blue'](s))
      } else if (c === 'users'){
        this.log(cli.chalk['magenta'](s))
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })

  .action(function (input, callback) {

	/*
	 * words() automatically trim @ so keep it first
	 */
	let fstChar = input.charAt(0).toString()

    let [ command, ...rest ] = words(input)
    let contents = rest.join(' ')

    if ( fstChar === "@" )
    	command = fstChar + command

    if (command === 'disconnect') {
        server.end(new Message({ username, command }).toJSON() + '\n')
    } else if ( command === 'echo' ) {
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
        prevCmd = command
    } else if ( command === 'broadcast' ) {
    	/*
    	 * broadcast command
    	 */
    	server.write(new Message({ username, command, contents }).toJSON() + '\n')
    	prevCmd = command
    } else if( command === 'users') {
    	/*
    	 * users command
    	 */
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
        prevCmd = command
    } else if( command.charAt(0).toString() === '@' ){
    	/*
    	 * @username command
    	 */
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
        prevCmd = command
    } else {
    	if (prevCmd === 'echo') {
            contents = command + contents
            command = prevCmd
            server.write(new Message({username, command, contents}).toJSON() + '\n')
        } else if (prevCmd === 'users') {
            contents = command + ' ' + contents
            command = prevCmd
            server.write(new Message({username, command, contents}).toJSON() + '\n')
        } else if (prevCmd.charAt(0).toString() === '@') {
            contents = command + ' ' + contents
            command = prevCmd
            server.write(new Message({username, command, contents}).toJSON() + '\n')
        } else if (prevCmd === 'broadcast') {
            contents = command + ' ' + contents
            command = prevCmd
            server.write(new Message({username, command, contents}).toJSON() + '\n')
        } else {
            this.log(`Command <${command}> was not recognized`)
        }
    }
    callback()
  })
