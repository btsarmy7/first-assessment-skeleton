import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let connectedUsers = []


cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> <host> <port>')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    server = connect({ host: args.host /*localhost*/, port: args.port /*8080*/ }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      connectedUsers.push(username)
      console.log(connectedUsers)
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })

  .action(function (input, callback) {
    const [ command, ...rest ] = words(input)
    const contents = rest.join(' ')
    if (command === 'disconnect') {
      let indexOfUser = connectedUsers.indexOf(username)
      connectedUsers.splice(indexOfUser, 1)
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast') {
      server.emit('broadcast', new Message({username, command, contents }).toJSON() + '\n')
    } else if (command === '@username'){

    } else if (command == 'users'){
        console.log(connectedUsers)     
    }
    else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
