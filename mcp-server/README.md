<<<<<<< HEAD
# MCP Server

## Overview
MCP server is a RESTful API backend for Claude Desktop, handling context-based requests. The project is built with Scala, Play Framework, and ZIO for asynchronous processing.

## Requirements
- JDK 11+
- SBT 1.9.x
- Scala 2.13.x

## Getting Started

### Setup
1. Clone this repository
2. Navigate to the project directory
3. Run sbt to start the SBT shell
4. Run compile to compile the project
5. Run un to start the server locally

The server will be available at http://localhost:9000.

### API Endpoints
- POST /api/context/update - Updates context data
- GET /api/context/:id - Fetches context data by ID

### Testing
Run tests using:
`
sbt test
`

## Project Structure

### Key Components
- **Controller Layer** - Handles HTTP requests and responses
- **Service Layer** - Contains business logic
- **Model Layer** - Defines domain models
- **Async Layer** - Manages asynchronous processing with ZIO
- **Utils** - Provides utility functions

### Technologies
- Play Framework for RESTful APIs
- ZIO for asynchronous processing
- Play JSON for JSON handling
- Logback for logging

## Development

### Adding New Endpoints
1. Add new route in conf/routes
2. Create controller method in appropriate controller
3. Implement service method
4. Add tests

### Configuration
Application configuration is located in conf/application.conf.

## License
This project is proprietary and confidential.
=======
# MCP Server

This directory contains the MCP server implementation.

## Features

- Server-side functionality
- Integration with the Scala SDK
>>>>>>> dee6e0947d9522eb8e7035d43ce27e00d55f4b6d
