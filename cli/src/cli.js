import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
//let connectedUsers = []
let prevCommand = ' ' 


cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> <host> <port>')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    server = connect({ host: args.host /*localhost*/, port: args.port /*8080*/ }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      //connectedUsers.push(username)
      callback()
    })
  

    server.on('data', (buffer) => {
      //this.log(Message.fromJSON(buffer).toString())
      let c = Message.fromJSON(buffer).command

      if(c === 'connect'){
        this.log(cli.chalk['gray'](Message.fromJSON(buffer).toString()))
      } else if (c === 'disconnect') {
        this.log(cli.chalk['red'](Message.fromJSON(buffer).toString()))
      } else if (c === 'echo'){
      this.log(cli.chalk['magenta'](Message.fromJSON(buffer).toString()))
      } else if (c === 'broadcast'){
        this.log(cli.chalk['cyan'](Message.fromJSON(buffer).toString()))
      } else if (c === '@'){
        this.log(cli.chalk['blue'](Message.fromJSON(buffer).toString()))
      } else if (c === 'users'){
        this.log(cli.chalk['white'](Message.fromJSON(buffer).toString()))
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })

  .action(function (input, callback) {
    const [ command, ...rest ] = words(input)
    const contents = rest.join(' ')
    if (command === 'disconnect') {
     // let indexOfUser = connectedUsers.indexOf(username)
      //connectedUsers.splice(indexOfUser, 1)
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      prevCommand = command
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast') {
      prevCommand = command
      server.write(new Message({username, command, contents }).toJSON() + '\n')
    } else if (command === '@username'){
      prevCommand = command
      server.write(new Message({username, command, contents }).toJSON() + '\n')
      callback() 
    } else if (command == 'users'){
      prevCommand = command
      //console.log(connectedUsers)
      server.write(new Message({username, command, contents }).toJSON() + '\n')    
    }else {

      /*cli
      .mode(prevCommand)
      .delimiter('previous command:')
      .action(function (input, callback) {
        server.write(new Message({username, command, contents }).toJSON() + '\n') */
        
      
      /*if (prevCommand === 'echo'){
        //contents = command + contents
       // command = prevCommand
        server.write(new Message({username, command, contents }).toJSON() + '\n')
      }else if (prevCommand === 'broadcast'){
        //contents = command + ' ' + contents
       // command = prevCommand
        server.write(new Message({username, command, contents }).toJSON() + '\n')
      } else if (prevCommand === '@'){
        //contents = command + ' ' + contents
        //command = prevCommand
        server.write(new Message({username, command, contents }).toJSON() + '\n')
        callback()
      } else if (prevCommand === 'users'){
        //contents = command + ' ' + contents
        server.write(new Message({username, command, contents }).toJSON() + '\n')
      }*/ //else {
        this.log(`Command <${command}> was not recognized`)
        }
      
   
   callback()
  })
